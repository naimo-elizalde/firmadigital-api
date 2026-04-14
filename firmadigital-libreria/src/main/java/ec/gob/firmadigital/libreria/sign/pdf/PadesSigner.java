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
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.signatures.SignatureUtil;
import ec.gob.firmadigital.libreria.exceptions.InvalidFormatException;
import ec.gob.firmadigital.libreria.sign.SignInfo;
import ec.gob.firmadigital.libreria.sign.Signer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mfernandez
 */
public class PadesSigner implements Signer {

    private static final Logger LOGGER = Logger.getLogger(PadesSigner.class.getName());

    @Override
    public List<SignInfo> getSigners(byte[] sign) throws InvalidFormatException, IOException {
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
                com.itextpdf.signatures.PdfPKCS7 pdfPKCS7;
                try {
                    pdfPKCS7 = signatureUtil.readSignatureData(signatureName);
                } catch (Exception e) {
                    e.printStackTrace();
                    LOGGER.log(Level.SEVERE, "El PDF contiene una firma corrupta o con un formato desconocido ({0}), se continua con las siguientes si las hubiese: {1}", new Object[]{signatureName, e});
                    continue;
                }
                Certificate[] signCertificateChain = pdfPKCS7.getSignCertificateChain();
                X509Certificate[] certChain = new X509Certificate[signCertificateChain.length];
                for (int i = 0; i < certChain.length; i++) {
                    certChain[i] = (X509Certificate) signCertificateChain[i];
                }
                SignInfo signInfo = new SignInfo(certChain, pdfPKCS7.getSignDate().getTime());
                signInfos.add(signInfo);
            }
            return signInfos;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "No se ha podido obtener la informacion de los firmantes del PDF: {0}", e);
            throw new InvalidFormatException("No se ha podido obtener la informacion de los firmantes del PDF", e);
        }
    }
}
