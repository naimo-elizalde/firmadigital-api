/*
 * Copyright (C) 2021 
 * Authors: Ricardo Arguello, Misael Fernández
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.*
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package ec.gob.firmadigital.libreria.sign.pdf;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfString;
import com.itextpdf.signatures.SignatureUtil;
import ec.gob.firmadigital.libreria.exceptions.InvalidFormatException;
import ec.gob.firmadigital.libreria.sign.SignInfo;
import ec.gob.firmadigital.libreria.sign.Signer;
import ec.gob.firmadigital.libreria.utils.BouncyCastleUtils;
import java.util.Arrays;
import java.util.Collection;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.util.Store;

/**
 *
 * @author mfernandez
 */
public class PadesSigner implements Signer {

    private static final Logger LOGGER = Logger.getLogger(PadesSigner.class.getName());

    @Override
    public List<SignInfo> getSigners(byte[] sign) throws InvalidFormatException, IOException {
        BouncyCastleUtils.initializeBouncyCastle();

        try (InputStream is = new ByteArrayInputStream(sign);
             PdfReader pdfReader = new PdfReader(is) {
                 @Override
                 public boolean hasRebuiltXref() {
                     return false;
                 }
             };
             PdfDocument pdfDocument = new PdfDocument(pdfReader)) {

            SignatureUtil signatureUtil = new com.itextpdf.signatures.SignatureUtil(pdfDocument);

            @SuppressWarnings("unchecked")
            List<String> names = signatureUtil.getSignatureNames();
            List<SignInfo> signInfos = new ArrayList<>();
            for (String signatureName : names) {
                try {
                    SignInfo signInfo = buildSignInfoFromIText(signatureUtil, signatureName);
                    signInfos.add(signInfo);
                } catch (Exception e) {
                    try {
                        SignInfo signInfo = buildSignInfoFromCms(signatureUtil, signatureName);
                        signInfos.add(signInfo);
                    } catch (Exception fallbackException) {
                        e.printStackTrace();
                        LOGGER.log(Level.SEVERE, "El PDF contiene una firma corrupta o con un formato desconocido ({0}), se continua con las siguientes si las hubiese: {1}", new Object[]{signatureName, fallbackException});
                    }
                }
            }
            return signInfos;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "No se ha podido obtener la informacion de los firmantes del PDF: {0}", e);
            throw new InvalidFormatException("No se ha podido obtener la informacion de los firmantes del PDF", e);
        }
    }

    private SignInfo buildSignInfoFromIText(SignatureUtil signatureUtil, String signatureName) throws Exception {
        com.itextpdf.signatures.PdfPKCS7 pdfPKCS7 = signatureUtil.readSignatureData(signatureName);
        Certificate[] signCertificateChain = pdfPKCS7.getSignCertificateChain();
        if (signCertificateChain == null || signCertificateChain.length == 0 || signCertificateChain[0] == null) {
            throw new IllegalStateException("La firma no contiene una cadena de certificado valida");
        }

        X509Certificate[] certChain = new X509Certificate[signCertificateChain.length];
        for (int i = 0; i < certChain.length; i++) {
            certChain[i] = (X509Certificate) signCertificateChain[i];
        }
        return new SignInfo(certChain, pdfPKCS7.getSignDate().getTime());
    }

    private SignInfo buildSignInfoFromCms(SignatureUtil signatureUtil, String signatureName) throws Exception {
        PdfString contents = signatureUtil.getSignatureDictionary(signatureName).getAsString(PdfName.Contents);
        if (contents == null) {
            throw new IllegalStateException("La firma no contiene contenido CMS");
        }

        byte[] cmsBytes = trimPaddingZeros(contents.getValueBytes());
        CMSSignedData signedData = new CMSSignedData(cmsBytes);
        SignerInformationStore signerInformationStore = signedData.getSignerInfos();
        Collection<SignerInformation> signers = signerInformationStore.getSigners();
        if (signers == null || signers.isEmpty()) {
            throw new IllegalStateException("La firma CMS no contiene firmantes");
        }

        SignerInformation signer = signers.iterator().next();
        Store<X509CertificateHolder> certStore = signedData.getCertificates();
        @SuppressWarnings("unchecked")
        Collection<X509CertificateHolder> certCollection = certStore.getMatches(signer.getSID());
        if (certCollection == null || certCollection.isEmpty()) {
            throw new IllegalStateException("La firma CMS no contiene el certificado del firmante");
        }

        X509CertificateHolder certificateHolder = certCollection.iterator().next();
        X509Certificate x509Certificate = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certificateHolder);

        Date signingTime = new Date();
        AttributeTable attributes = signer.getSignedAttributes();
        if (attributes != null) {
            Attribute signingTimeAttribute = attributes.get(CMSAttributes.signingTime);
            if (signingTimeAttribute != null && signingTimeAttribute.getAttrValues().size() > 0) {
                try {
                    signingTime = ((org.bouncycastle.asn1.ASN1UTCTime) signingTimeAttribute.getAttrValues().getObjectAt(0)).getDate();
                } catch (Exception ignored) {
                    // Si el atributo no se puede convertir, se usa la fecha actual.
                }
            }
        }

        return new SignInfo(new X509Certificate[]{x509Certificate}, signingTime);
    }

    private byte[] trimPaddingZeros(byte[] cmsBytes) {
        if (cmsBytes == null || cmsBytes.length == 0) {
            return new byte[0];
        }

        int end = cmsBytes.length;
        while (end > 0 && cmsBytes[end - 1] == 0) {
            end--;
        }
        return Arrays.copyOf(cmsBytes, end);
    }
}
