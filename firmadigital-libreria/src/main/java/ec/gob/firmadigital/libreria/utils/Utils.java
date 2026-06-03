/*
 * Copyright (C) 2020 
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
package ec.gob.firmadigital.libreria.utils;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDate;
import com.itextpdf.kernel.pdf.PdfDictionary;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.openmbean.InvalidKeyException;
import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.tsp.TimeStampToken;
import org.w3c.dom.Node;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.utils.CompareTool;
import com.itextpdf.signatures.PdfPKCS7;
import com.itextpdf.signatures.SignatureUtil;

import static ec.gob.firmadigital.libreria.utils.CheckPDF.checkPDF;
import ec.gob.firmadigital.libreria.certificate.CertEcUtils;
import ec.gob.firmadigital.libreria.certificate.to.Certificado;
import ec.gob.firmadigital.libreria.certificate.to.DatosUsuario;
import ec.gob.firmadigital.libreria.certificate.to.Documento;
import ec.gob.firmadigital.libreria.core.Util;
import ec.gob.firmadigital.libreria.exceptions.CRLValidationException;
import ec.gob.firmadigital.libreria.exceptions.CertificadoInvalidoException;
import ec.gob.firmadigital.libreria.exceptions.ConexionException;
import ec.gob.firmadigital.libreria.exceptions.ConexionInvalidaOCSPException;
import ec.gob.firmadigital.libreria.exceptions.ConexionValidarCRLException;
import ec.gob.firmadigital.libreria.exceptions.DocumentoException;
import ec.gob.firmadigital.libreria.exceptions.EntidadCertificadoraNoValidaException;
import ec.gob.firmadigital.libreria.exceptions.HoraServidorException;
import ec.gob.firmadigital.libreria.exceptions.OcspValidationException;
import ec.gob.firmadigital.libreria.exceptions.RubricaException;
import ec.gob.firmadigital.libreria.exceptions.SignatureVerificationException;
import ec.gob.firmadigital.libreria.sign.SignInfo;
import ec.gob.firmadigital.libreria.sign.Signer;
import ec.gob.firmadigital.libreria.sign.cms.VerificadorCMS;
import ec.gob.firmadigital.libreria.sign.pdf.PadesSigner;
import ec.gob.firmadigital.libreria.sign.xades.XAdESSigner;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.util.Store;

/**
 * M&eacute;todos generales de utilidad para toda la aplicaci&oacute;n.
 */
public class Utils {

    private static final int BUFFER_SIZE = 4096;
    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Obtiene el flujo de entrada de un fichero (para su lectura) a partir de
     * su URI.
     *
     * @param uri URI del fichero a leer
     * @return Flujo de entrada hacia el contenido del fichero
     * @throws IOException Cuando no se ha podido abrir el fichero de datos.
     */
    public static InputStream loadFile(URI uri) throws IOException {

        if (uri == null) {
            throw new IllegalArgumentException("Se ha pedido el contenido de una URI nula");
        }

        if (uri.getScheme().equals("file")) {
            // Es un fichero en disco. Las URL de Java no soportan file://, con
            // lo que hay que diferenciarlo a mano

            // Retiramos el "file://" de la uri
            String path = uri.getSchemeSpecificPart();
            if (path.startsWith("//")) {
                path = path.substring(2);
            }
            return new FileInputStream(new File(path));
        }

        // Es una URL
        byte[] tmpBuffer;
        try (InputStream tmpStream = new BufferedInputStream(uri.toURL().openStream())) {
            tmpBuffer = getDataFromInputStream(tmpStream);
        }
        return new ByteArrayInputStream(tmpBuffer);
    }

    public static byte[] getBytesFromFile(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    /**
     * Lee un flujo de datos de entrada y los recupera en forma de array de
     * bytes. Este m&eacute;todo consume pero no cierra el flujo de datos de
     * entrada.
     *
     * @param input Flujo de donde se toman los datos.
     * @return Los datos obtenidos del flujo.
     * @throws IOException Cuando ocurre un problema durante la lectura
     */
    public static byte[] getDataFromInputStream(InputStream input) throws IOException {
        if (input == null) {
            return new byte[0];
        }
        int nBytes;
        byte[] buffer = new byte[BUFFER_SIZE];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((nBytes = input.read(buffer)) != -1) {
            baos.write(buffer, 0, nBytes);
        }
        return baos.toByteArray();
    }

    /**
     * Obtiene el UID del titular de un certificado X.509. Si no se encuentra el
     * CN, se devuelve la unidad organizativa (Organization Unit, OU).
     *
     * @param c Certificado X.509 del cual queremos obtener el nombre
     * com&uacute;n
     * @return Nombre com&uacute;n (Common Name, UID) del titular de un
     * certificado X.509
     */
    public static String getUID(X509Certificate c) {
        if (c == null) {
            return null;
        }
        return getUID(c.getSubjectX500Principal().toString());
    }

    /**
     * Obtiene el UID de un <i>Principal</i>
     * X.400. Si no se encuentra el CN, se devuelve la unidad organizativa
     * (Organization Unit, OU).
     *
     * @param principal <i>Principal</i> del cual queremos obtener el nombre
     * com&uacute;n
     * @return Nombre com&uacute;n (Common Name, UID) de un <i>Principal</i>
     * X.400
     */
    public static String getUID(final String principal) {
        if (principal == null) {
            return null;
        }

        String rdn = getRDNvalueFromLdapName("uid", principal);
        if (rdn != null) {
            return rdn;
        }

        final int i = principal.indexOf('=');
        if (i != -1) {
            LOGGER.warning(
                    "No se ha podido obtener el UID, se devolvera el fragmento mas significativo");
            return getRDNvalueFromLdapName(principal.substring(0, i), principal);
        }

        LOGGER.warning("Principal no valido, se devolvera la entrada");
        return principal;
    }

    /**
     * Obtiene el nombre com&uacute;n (Common Name, CN) del titular de un
     * certificado X.509. Si no se encuentra el CN, se devuelve la unidad
     * organizativa (Organization Unit, OU).
     *
     * @param c Certificado X.509 del cual queremos obtener el nombre
     * com&uacute;n
     * @return Nombre com&uacute;n (Common Name, CN) del titular de un
     * certificado X.509
     */
    public static String getCN(X509Certificate c) {
        if (c == null) {
            return null;
        }
        return getCN(c.getSubjectX500Principal().toString());
    }

    /**
     * Obtiene el nombre com&uacute;n (Common Name, CN) de un <i>Principal</i>
     * X.400. Si no se encuentra el CN, se devuelve la unidad organizativa
     * (Organization Unit, OU).
     *
     * @param principal <i>Principal</i> del cual queremos obtener el nombre
     * com&uacute;n
     * @return Nombre com&uacute;n (Common Name, CN) de un <i>Principal</i>
     * X.400
     */
    public static String getCN(final String principal) {
        if (principal == null) {
            return null;
        }

        String rdn = getRDNvalueFromLdapName("cn", principal);
        if (rdn == null) {
            rdn = getRDNvalueFromLdapName("ou", principal);
        }

        if (rdn != null) {
            return rdn;
        }

        final int i = principal.indexOf('=');
        if (i != -1) {
            LOGGER.warning(
                    "No se ha podido obtener el Common Name ni la Organizational Unit, se devolvera el fragmento mas significativo");
            return getRDNvalueFromLdapName(principal.substring(0, i), principal);
        }

        LOGGER.warning("Principal no valido, se devolvera la entrada");
        return principal;
    }

    /**
     * Recupera el valor de un RDN (<i>Relative Distinguished Name</i>) de un
     * principal. El valor de retorno no incluye el nombre del RDN, el igual, ni
     * las posibles comillas que envuelvan el valor. La funci&oacute;n no es
     * sensible a la capitalizaci&oacute;n del RDN. Si no se encuentra, se
     * devuelve {@code null}.
     *
     * @param rdn RDN que deseamos encontrar.
     * @param principal Principal del que extraer el RDN (seg&uacute;n la
     * <a href="http://www.ietf.org/rfc/rfc4514.txt">RFC 4514</a>).
     * @return Valor del RDN indicado o {@code null} si no se encuentra.
     */
    public static String getRDNvalueFromLdapName(final String rdn, final String principal) {
        int offset1 = 0;

        while ((offset1 = principal.toLowerCase(Locale.US).indexOf(rdn.toLowerCase(), offset1)) != -1) {
            if (offset1 > 0 && principal.charAt(offset1 - 1) != ',' && principal.charAt(offset1 - 1) != ' ') {
                offset1++;
                continue;
            }

            offset1 += rdn.length();
            while (offset1 < principal.length() && principal.charAt(offset1) == ' ') {
                offset1++;
            }

            if (offset1 >= principal.length()) {
                return null;
            }

            if (principal.charAt(offset1) != '=') {
                continue;
            }

            offset1++;
            while (offset1 < principal.length() && principal.charAt(offset1) == ' ') {
                offset1++;
            }

            if (offset1 >= principal.length()) {
                return "";
            }

            int offset2;
            switch (principal.charAt(offset1)) {
                case ',':
                    return "";
                case '"':
                    offset1++;
                    if (offset1 >= principal.length()) {
                        return "";
                    }

                    offset2 = principal.indexOf('"', offset1);
                    if (offset2 == offset1) {
                        return "";
                    } else if (offset2 != -1) {
                        return principal.substring(offset1, offset2);
                    } else {
                        return principal.substring(offset1);
                    }
                default:
                    offset2 = principal.indexOf(',', offset1);
                    if (offset2 != -1) {
                        return principal.substring(offset1, offset2).trim();
                    }
                    return principal.substring(offset1).trim();
            }
        }

        return null;
    }

    public static X509Certificate getCertificate(Node certificateNode) {
        return createCert(certificateNode.getTextContent().trim().replace("\r", "").replace("\n", "").replace(" ", "")
                .replace("\t", ""));
    }

    public static X509Certificate createCert(String b64Cert) {
        if (b64Cert == null || b64Cert.isEmpty()) {
            LOGGER.severe("Se ha proporcionado una cadena nula o vacia, se devolvera null");
            return null;
        }
        X509Certificate cert;
        try (InputStream isCert = new ByteArrayInputStream(Base64.getDecoder().decode(b64Cert))) {
            cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(isCert);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "No se pudo decodificar el certificado en Base64, se devolvera null: {0}", e);
            return null;
        }
        return cert;
    }

    public static Date getSignTime(String fechaHora) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        try {
            TemporalAccessor accessor = timeFormatter.parse(fechaHora);
            return Date.from(Instant.from(accessor));
        } catch (DateTimeParseException e) {
            LOGGER.log(Level.SEVERE, "La fecha indicada (''{0}'') como momento de firma para PDF no sigue el patron ISO-8601: {1}", new Object[]{fechaHora, e});
            return new Date();
        }
    }

    public static List<Certificado> datosP7mToCertificado(List<X509Certificate> certificados, List<Date> fechasFirmados) throws RubricaException, HoraServidorException, IOException, CertificadoInvalidoException, EntidadCertificadoraNoValidaException, ConexionValidarCRLException, CRLValidationException, OcspValidationException, ConexionException {
        List<Certificado> tempCertificados = new ArrayList<>();
        for (int i = 0; i < certificados.size(); i++) {
            X509Certificate temp = certificados.get(i);
            Date fechaFirmado = fechasFirmados.get(i);
            DatosUsuario datosUsuario = CertEcUtils.getDatosUsuarios(temp);
            if (datosUsuario == null) {//documentos del 2012 para atrás
                datosUsuario = new DatosUsuario();
                datosUsuario.setCedula("No se puede obtener");
                datosUsuario.setNombre(Util.getCN(temp));
            }
            datosUsuario.setFechaFirmaArchivoP7M((String) new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((dateToCalendar(fechaFirmado)).getTime()));
            Certificado certificado = new Certificado(
                    temp.getSerialNumber().toString(),
                    Util.getCN(temp),
                    CertEcUtils.getNombreCA(temp),
                    dateToCalendar(temp.getNotBefore()),
                    dateToCalendar(temp.getNotAfter()),
                    dateToCalendar(fechaFirmado),
                    dateToCalendar(UtilsCrlOcsp.validarFechaRevocado(temp, null)),
                    esValido(temp, fechaFirmado),
                    datosUsuario);
            certificado.setDocValidTimeStamp(false);

            tempCertificados.add(certificado);
        }
        return tempCertificados;
    }

    //revertir
    public static Documento pdfToDocumento(InputStream pdf) throws IOException, SignatureVerificationException, Exception {
        PdfReader pdfReader = new PdfReader(pdf);
        Signer signer = new PadesSigner();
        java.util.List<SignInfo> signInfos;
        signInfos = signer.getSigners(pdf.readAllBytes());
        return pdfToDocumento(pdfReader, signInfos);
    }

    public static Documento pdfToDocumento(File pdf) throws IOException, SignatureVerificationException, Exception {
        PdfReader pdfReader = new PdfReader(pdf);
        java.util.List<SignInfo> signInfos;
        Signer signer = new PadesSigner();
        signInfos = signer.getSigners(FileUtils.fileConvertToByteArray(pdf));
        return pdfToDocumento(pdfReader, signInfos);
    }

    public static Documento pdfToDocumento(PdfReader pdfReader, java.util.List<SignInfo> signInfos) throws IOException, SignatureVerificationException, Exception {
        Documento documento;
        try (PdfDocument pdfDocument = new PdfDocument(pdfReader)) {
            SignatureUtil signatureUtil = new SignatureUtil(pdfDocument);
            List<Certificado> certificados = new ArrayList<>();
            documento = new Documento(true, false, certificados, null);
            if (signInfos == null || signInfos.isEmpty()) {
                return new Documento(false, false, certificados, "Documento sin firmas");
            } else {
                for (SignInfo signInfo : signInfos) {
                    Certificado certificado = signInfoToCertificado(signInfo);
                    try {
                        java.util.List<String> signatureNames = signatureUtil.getSignatureNames();
                        for (String signatureName : signatureNames) {
                            PdfPKCS7 pdfPKCS7 = signatureUtil.readSignatureData(signatureName);
                            for (X509Certificate certificate : signInfo.getCerts()) {
                                if (pdfPKCS7.getSigningCertificate().equals(certificate)
                                        && certificado.getSignGenerated().getTime().equals(pdfPKCS7.getSignDate().getTime())) {
                                    // Validacion Sellado de Tiempo
                                    TimeStampToken tsToken = pdfPKCS7.getTimeStampToken();
                                    if (tsToken != null) { // Timestamping Change Openpdf to itext
                                        ////////////////////
                                        Store store = tsToken.getCertificates();
                                        Collection collection = store.getMatches(tsToken.getSID());
                                        Iterator iterator = collection.iterator();
                                        X509CertificateHolder certificateHolder = (X509CertificateHolder) iterator.next();
                                        X509Certificate x509Certificate = new JcaX509CertificateConverter().getCertificate(certificateHolder);
                                        ////////////////////
                                        boolean tsTohenisSignatureValid = tsToken.isSignatureValid(new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(certificateHolder));
                                        tsToken.validate(new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(certificateHolder));
                                        if (tsTohenisSignatureValid) {
                                            List<String> extendedKeyUsages = x509Certificate.getExtendedKeyUsage();
                                            for (String extendedKeyUsage : extendedKeyUsages) {
                                                if (extendedKeyUsage.equals("1.3.6.1.5.5.7.3.8")) {//oid timestamping
                                                    certificado.getDatosUsuario().setApellido("");
                                                    certificado.setDocTimeStamp(tsToken.getTimeStampInfo().getGenTime());
                                                    certificado.setDocTimeStampIssuedBy(CertEcUtils.getNombreCA(x509Certificate));
                                                    certificado.setCnTimeStamp(getCN(x509Certificate));
                                                    certificado.setDatosUsuario(infoCertificado(certificado.getDatosUsuario(), signInfo));
                                                    try {
                                                        if (verifySignature(x509Certificate)) {
                                                            certificado.setDocValidTimeStamp(true);
                                                        }
                                                    } catch (EntidadCertificadoraNoValidaException ex) {
                                                    }

                                                }
                                            }
                                        }
                                    }
                                    // Validacion Sellado de Tiempo
                                    certificado.setDocReason(pdfPKCS7.getReason());
                                    certificado.setDocLocation(pdfPKCS7.getLocation());
                                    certificado.setSignVerify(pdfPKCS7.verifySignatureIntegrityAndAuthenticity());
                                    //documento sin ser modificado
                                    if (!documento.getDocValidate()) {
//                                        System.out.println("--------------signatureName " + signatureName + "--------------");
//                                        InputStream revisionStream = signatureUtil.extractRevision(signatureName);
//                                        PdfReader pdfReaderRevision = new PdfReader(revisionStream);
//                                        try (PdfDocument pdfDocumentRevision = new PdfDocument(pdfReaderRevision)) {
//                                            infoPDF(pdfDocumentRevision);
//                                            System.out.println("----------------------------");
//                                        }
                                        documento.setDocValidate(signatureUtil.signatureCoversWholeDocument(signatureName));
                                    }
                                    // Obtiene KeyUsages
                                    certificado.setKeyUsages(validacionKeyUsages(pdfPKCS7.getSigningCertificate()));
                                    break;
                                }
                            }
                        }
                    } catch (SignatureException ex) {
                        LOGGER.log(Level.SEVERE, ex.getMessage());
                    }
                    certificados.add(certificado);
                }
                if (certificados != null || !certificados.isEmpty()) {
                    documento.setSignValidate(validarCertificados(certificados, true));
                }
                documento.setCertificados(certificados);
            }
        }
        return documento;
    }

    private static void infoPDF(PdfDocument pdfDocument) {
        try {
            SignatureUtil signatureUtil = new SignatureUtil(pdfDocument);
            List<String> signatureNames = signatureUtil.getSignatureNames();
            if (signatureNames.isEmpty()) {
                System.out.println("No signed revisions detected. (no AcroForm)");
            } else {
                CompareTool compareTool = new CompareTool();
                PdfDocument currentDocument = null;
                PdfDocument nextDocument = null;
                for (int i = 1; i < signatureNames.size(); i++) {
                    String currentRevision = signatureNames.get(i - 1);
                    System.out.printf("* Signed revision (%d): %s\n", i, currentRevision);
                    currentDocument = new PdfDocument(new PdfReader(signatureUtil.extractRevision(currentRevision)));

                    String nextRevision = signatureNames.get(i);
                    nextDocument = new PdfDocument(new PdfReader(signatureUtil.extractRevision(nextRevision)));

                    if (signatureUtil.signatureCoversWholeDocument(nextRevision)) {
                        System.out.printf("* Signed revision (%d): %s\n", i, "No unsigned updates.");
                    }

                    if (currentDocument.getNumberOfPages() != nextDocument.getNumberOfPages()) {
                        System.out.printf("* Signed revision (%d): %s\n", i, "Distinto número de paginas.");
                    }
                    compareTool.disableCachedPagesComparison();
                    compareTool.enableEncryptionCompare();
                    CompareTool.CompareResult compareResult = compareTool.compareByCatalog(currentDocument, nextDocument);
                    System.out.println("compareResult.getReport(): " + compareResult.getReport());
                }
                if (currentDocument != null && nextDocument != null) {
                    currentDocument.close();
                    nextDocument.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //get metadata map
        PdfDictionary trailerDictionary = pdfDocument.getTrailer();
        trailerDictionary.entrySet().forEach(entry -> {
            if (entry.getKey().getValue().equals("Prev")) {
                System.out.println(entry.getKey().getValue() + " - " + entry.getValue());
            }
            if (entry.getKey().getValue().equals("XRefStm")) {
                System.out.println(entry.getKey().getValue() + " - " + entry.getValue());
            }
        });
        PdfDictionary infoDictionary = pdfDocument.getTrailer().getAsDictionary(PdfName.Info);
        infoDictionary.entrySet().forEach(entry -> {
            if (entry.getKey().getValue().equals("Author")) {
                System.out.println(entry.getKey().getValue() + " - " + entry.getValue());
            }
            if (entry.getKey().getValue().equals("Creator")) {
                System.out.println(entry.getKey().getValue() + " - " + entry.getValue());
            }
            if (entry.getKey().getValue().equals("Producer")) {
                System.out.println(entry.getKey().getValue() + " - " + entry.getValue());
            }
            if (entry.getKey().getValue().equals("CreationDate")) {
                System.out.println("CreationDate: " + PdfDate.decode(entry.getValue().toString()).getTime());
            }
            if (entry.getKey().getValue().equals("ModDate")) {
                System.out.println("ModDate: " + PdfDate.decode(entry.getValue().toString()).getTime());
            }
        });
        //get metadata map
    }

    /**
     * Valida que los keyusage sean por lo menos digitalSignature y
     * NonRepudiation
     *
     * @param signCert
     * @return
     */
    public static String validacionKeyUsages(X509Certificate signCert) throws CertificateParsingException {
        String keyUsages = "";
        boolean[] keyUsage = signCert.getKeyUsage();
        if (keyUsage[0]) {
            keyUsages += "Firma Electrónica, ";// digitalSignature
        }
        if (keyUsage[1]) {
            keyUsages += "No Repudio, "; // nonRepudiation
        }
        if (keyUsage[2]) {
            keyUsages += "Cifrado de llave, ";// keyEncipherment
        }
        if (keyUsage[3]) {
            keyUsages += "Cifrado de datos, ";// dataEncipherment
        }
        if (keyUsage[4]) {
            keyUsages += "Acuerdo de llaves, "; // keyAgreement
        }
        if (keyUsage[5]) {
            keyUsages += "Firma y certificado de llave, ";// keyCertSign
        }
        if (keyUsage[6]) {
            keyUsages += "Firma de CRL, ";// cRLSign
        }
        if (keyUsage[7]) {
            keyUsages += "Solo cifrado, ";// encipherOnly
        }
        if (keyUsage[8]) {
            keyUsages += "Solo descifrado"; // decipherOnly
        }
        return keyUsages;
    }

    private static final PdfName PDFNAME_ETSI_RFC3161 = new PdfName("ETSI.RFC3161");
    private static final PdfName PDFNAME_DOCTIMESTAMP = new PdfName("DocTimeStamp");

    public static Certificado isTimeStamping(SignatureUtil signatureUtil, String signatureName, Certificado certificado) throws CertificateParsingException, SignatureException {
        PdfPKCS7 pdfPKCS7 = signatureUtil.readSignatureData(signatureName);
        PdfDictionary pdfDictionary = signatureUtil.getSignatureDictionary(signatureName);
        if (PDFNAME_ETSI_RFC3161.equals(pdfDictionary.get(PdfName.SubFilter))
                || PDFNAME_DOCTIMESTAMP.equals(pdfDictionary.get(PdfName.SubFilter))) {
            List<String> extendedKeyUsages = pdfPKCS7.getSigningCertificate().getExtendedKeyUsage();
            for (String extendedKeyUsage : extendedKeyUsages) {
                if (extendedKeyUsage.equals("1.3.6.1.5.5.7.3.8")) {//oid timestamping
                    certificado.setDocValidTimeStamp(true);
                }
            }
        }
        return certificado;
    }

    public static DatosUsuario infoCertificado(DatosUsuario datosUsuario, SignInfo signInfo) {
        X500Principal subjectX500Principal = signInfo.getCerts()[0].getSubjectX500Principal();
        X500Name subjectX500name = new X500Name(subjectX500Principal.getName());
        try {
            // Cedula (SERIALNUMBER)
            if (subjectX500name.getRDNs(BCStyle.SERIALNUMBER).length > 0) {
                datosUsuario.setCedula(subjectX500name.getRDNs(BCStyle.SERIALNUMBER)[0].getFirst().getValue().toString());
            }
            // Nombre (GIVENNAME o CN como fallback)
            if (subjectX500name.getRDNs(BCStyle.GIVENNAME).length > 0) {
                datosUsuario.setNombre(subjectX500name.getRDNs(BCStyle.GIVENNAME)[0].getFirst().getValue().toString());
            } else if (subjectX500name.getRDNs(BCStyle.CN).length > 0) {
                datosUsuario.setNombre(subjectX500name.getRDNs(BCStyle.CN)[0].getFirst().getValue().toString());
            }
            // Apellido (SURNAME)
            if (subjectX500name.getRDNs(BCStyle.SURNAME).length > 0) {
                datosUsuario.setApellido(subjectX500name.getRDNs(BCStyle.SURNAME)[0].getFirst().getValue().toString());
            }
            // Institucion (O = Organization)
            if (subjectX500name.getRDNs(BCStyle.O).length > 0) {
                datosUsuario.setInstitucion(subjectX500name.getRDNs(BCStyle.O)[0].getFirst().getValue().toString());
            }
            // Cargo (T = Title, fallback OU)
            if (subjectX500name.getRDNs(BCStyle.T).length > 0) {
                datosUsuario.setCargo(subjectX500name.getRDNs(BCStyle.T)[0].getFirst().getValue().toString());
            } else if (subjectX500name.getRDNs(BCStyle.OU).length > 0) {
                datosUsuario.setCargo(subjectX500name.getRDNs(BCStyle.OU)[0].getFirst().getValue().toString());
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error extrayendo datos del Subject DN: {0}", e.getMessage());
        }
        return datosUsuario;
    }

    public static Certificado signInfoToCertificado(SignInfo signInfo) throws CertificadoInvalidoException, IOException, ConexionException, EntidadCertificadoraNoValidaException {
        signInfo.getCerts();
        Certificado certificado = null;
        DatosUsuario datosUsuario = CertEcUtils.getDatosUsuarios(signInfo.getCerts()[0]);
        if (datosUsuario == null) {
            //creando objeto para leer info
            datosUsuario = new DatosUsuario();
            datosUsuario = infoCertificado(datosUsuario, signInfo);
            datosUsuario.setCertificadoDigitalValido(false);
        }
        certificado = new Certificado(
                signInfo.getCerts()[0].getSerialNumber().toString(),
                Util.getCN(signInfo.getCerts()[0]),
                CertEcUtils.getNombreCA(signInfo.getCerts()[0]),
                dateToCalendar(signInfo.getCerts()[0].getNotBefore()),
                dateToCalendar(signInfo.getCerts()[0].getNotAfter()),
                dateToCalendar(signInfo.getSigningTime()),
                dateToCalendar(UtilsCrlOcsp.validarFechaRevocado(signInfo.getCerts()[0], null)),
                esValido(signInfo.getCerts()[0], signInfo.getSigningTime()),
                datosUsuario);
        certificado.setDocValidTimeStamp(false);
        return certificado;
    }

    public static Calendar dateToCalendar(Date date) {
        Calendar calendar = null;
        if (date != null) {
            calendar = Calendar.getInstance();
            calendar.setTime(date);
        }
        return calendar;
    }

    /**
     * Si el certificado ya caduco
     *
     * @param cert
     * @param signingTime
     * @return
     */
    public static boolean esValido(X509Certificate cert, Date signingTime) {
        return !(signingTime.before(cert.getNotBefore()) || signingTime.after(cert.getNotAfter()));
    }

    public static Documento x509CertificateToDocumento(java.util.List<SignInfo> signInfos)
            throws DocumentoException, CertificadoInvalidoException, IOException, ConexionException, EntidadCertificadoraNoValidaException, CertificateParsingException {
        Documento documento = null;
        List<Certificado> certificados = new ArrayList<>();

        if (signInfos == null || signInfos.isEmpty()) {
            return new Documento(false, false, certificados, "Documento sin firmas");
        } else {
            for (SignInfo signInfo : signInfos) {
                documento = new Documento(true, false, null, null);
                Certificado certificado = signInfoToCertificado(signInfo);
                if (certificado == null) {
                    return new Documento(false, false, certificados, "Documento sin firmas");
                } else {
                    certificados.add(certificado);
                }
            }
        }
        documento.setCertificados(certificados);
        documento.setSignValidate(validarCertificados(documento.getCertificados(), false));
        return documento;
    }

//    private static Certificado validarX509Certificate(SignInfo signInfo)
//            throws EntidadCertificadoraNoValidaException, CertificadoInvalidoException, IOException, ConexionException, CertificateParsingException {
//        DatosUsuario datosUsuario = CertEcUtils.getDatosUsuarios(x509Certificate);
//        datosUsuario.setFechaFirmaArchivoP7M((String) new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((dateToCalendar(fechaFirmado)).getTime()));
//        Certificado certificado = new Certificado(
//                x509Certificate.getSerialNumber().toString(),
//                Util.getCN(x509Certificate),
//                CertEcUtils.getNombreCA(x509Certificate),
//                Utils.dateToCalendar(x509Certificate.getNotBefore()),
//                Utils.dateToCalendar(x509Certificate.getNotAfter()),
//                null,
//                Utils.dateToCalendar(UtilsCrlOcsp.validarFechaRevocado(x509Certificate, null)),
//                esValido(x509Certificate, fechaFirmado),
//                datosUsuario);
//        certificado.setKeyUsages(Utils.validacionKeyUsages(x509Certificate));
//        return certificado;
//    }
    public static Documento verificarDocumento(File file, String base64) throws IOException, KeyStoreException, OcspValidationException, SignatureException, RubricaException, ConexionInvalidaOCSPException, HoraServidorException, CertificadoInvalidoException, EntidadCertificadoraNoValidaException, ConexionValidarCRLException, SignatureVerificationException, DocumentoException, CRLValidationException, Exception {
        byte[] docByteArray = FileUtils.fileConvertToByteArray(file);
        // para P7m, ya que p7m no tiene signer
        Documento documento = null;
        String extDocumento = FileUtils.getExtension(docByteArray);
        switch (extDocumento.toLowerCase()) {
            case ".p7s" -> {
                VerificadorCMS verificador = new VerificadorCMS();
                byte[] archivoOriginal = verificador.verify(docByteArray);
                String nombreArchivo = FileUtils.crearNombreVerificado(file, FileUtils.getExtension(archivoOriginal), base64);
                FileUtils.saveByteArrayToDisc(archivoOriginal, nombreArchivo);
                FileUtils.abrirDocumento(nombreArchivo);
                documento = new Documento(true, false, null, null);
                documento.setCertificados(Utils.datosP7mToCertificado(verificador.certificados, verificador.fechasFirmados));
                documento.setSignValidate(validarCertificados(documento.getCertificados(), false));
                return documento;
            }
            case ".pdf" -> {
                PdfReader pdfReader = new PdfReader(file);
                try (PdfDocument pdfDocumentRevision = new PdfDocument(pdfReader)) {
                    String mensajeAnalisisDocumento = checkPDF(pdfDocumentRevision);
                    if (mensajeAnalisisDocumento != null) {
                        List<Certificado> certificados = new ArrayList<>();
                        return new Documento(false, false, certificados, mensajeAnalisisDocumento);
                    } else {
                        return Utils.pdfToDocumento(file);
                    }
                }
            }
            case ".xml" -> {
                {
                    XAdESSigner xAdESSigner = new XAdESSigner();
                    java.util.List<SignInfo> signInfos;
                    signInfos = xAdESSigner.getSignInfo(docByteArray);
                    documento = Utils.x509CertificateToDocumento(signInfos);
                    //SRI
                    //                String xml = leerXmlSRI(documento);
                    //                List<Certificado> certificadosSRI = Utils.signInfosToCertificados(docSigner.getSigners(xml.getBytes(StandardCharsets.UTF_8)));
                    //                if (!certificadosSRI.isEmpty()) {
                    //                    jakarta.swing.JOptionPane.showMessageDialog(null, PropertiesUtils.getMessages().getProperty("mensaje.error.documento_sri"), "Advertencia", jakarta.swing.JOptionPane.WARNING_MESSAGE);
                    //                }
                    //                certificados.addAll(certificadosSRI);
                    //SRI
                }
                return documento;
            }
            default -> {
                List<Certificado> certificados = new ArrayList<>();
                return new Documento(false, false, certificados, "El archivo no se puede validar, es extensión " + extDocumento);
            }
        }
    }

    private static boolean validarCertificados(List<Certificado> certificados, boolean pdf) {
        boolean retorno = true;
        if (certificados != null || !certificados.isEmpty()) {
            for (Certificado certificado : certificados) {
                if (!certificado.getDocValidTimeStamp()) {//certificados digitales
                    //certificado digital sin ser revocado, integridad de la firma, dentro de fecha de figencia, válido por CA
                    boolean revocado = validarFirma(certificado.getValidFrom(), certificado.getValidTo(), certificado.getSignGenerated(), certificado.getRevocated());
                    if (pdf) {
                        if (!revocado || !certificado.getSignVerify() || !certificado.getCertificateValidated()) {
                            retorno = false;
                            break;
                        }
                    } else {
                        if (!revocado || !certificado.getCertificateValidated()) {
                            retorno = false;
                            break;
                        }
                    }
                } else {// sellos de tiempo
                    //dentro de fecha de figencia, válido por CA
                    if (!certificado.getCertificateValidated()) {
                        retorno = false;
                        break;
                    }
                }
            }
        }
        return retorno;
    }

    public static String leerXmlSRI(File documento) {
        Scanner entrada = null;
        //String xml
        String xml = "";
        try {
            //creamos un Scanner para leer el fichero
            entrada = new Scanner(documento);
            while (entrada.hasNext()) { //mientras no se llegue al final del fichero
                xml += entrada.nextLine();  //se lee una línea
            }
            xml = xml.replaceAll("&lt;", "<");
            xml = xml.replaceAll("&gt;", ">");
            //Texto a buscar
            String inicioTexto1 = "<comprobante><![CDATA[";
            String finTexto1 = "]]></comprobante>";
            String inicioTexto2 = "<comprobante>";
            String finTexto2 = "</comprobante>";
            if (xml.contains(inicioTexto1)) {   //si la línea contiene el texto buscado
                xml = xml.substring(xml.lastIndexOf(inicioTexto1) + inicioTexto1.length(),
                        xml.indexOf(finTexto1));
            } else {   //si la línea contiene el texto buscado
                xml = xml.substring(xml.lastIndexOf(inicioTexto2) + inicioTexto2.length(),
                        xml.indexOf(finTexto2));
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        } finally {
            if (entrada != null) {
                entrada.close();
            }
        }
        return xml;
    }

    public static boolean validarFirma(Calendar fechaDesde, Calendar fechaHasta, Calendar fechaFirmado, Calendar fechaRevocado) {
        boolean retorno = true;
        if (fechaRevocado == null) {
            retorno = true;
        } else {
            if (fechaFirmado.compareTo(fechaDesde) >= 0 && fechaFirmado.compareTo(fechaHasta) <= 0) {
                if (fechaRevocado != null && fechaRevocado.compareTo(fechaFirmado) <= 0) {
                    retorno = false;
                }
            } else {
                retorno = false;
            }
        }
        return retorno;
    }

    /**
     * Verifies that the given certificate was signed using the private key that
     * corresponds to the public key of the provided certificate.
     *
     * @param certificate The X509Certificate which is to be checked
     * @return True, if the verification was successful, false otherwise
     * @throws java.security.InvalidKeyException
     * @throws
     * ec.gob.firmadigital.libreria.exceptions.EntidadCertificadoraNoValidaException
     */
    public static boolean verifySignature(X509Certificate certificate) throws java.security.InvalidKeyException, EntidadCertificadoraNoValidaException {
        return verifySignature(certificate, CertEcUtils.getRootCertificate(certificate));
    }

    public static boolean verifySignature(X509Certificate certificate, X509Certificate rootCertificate) throws java.security.InvalidKeyException, EntidadCertificadoraNoValidaException {
        if (rootCertificate != null) {
            PublicKey publicKeyForSignature = rootCertificate.getPublicKey();

            try {
                certificate.verify(publicKeyForSignature);
                return true;
            } catch (InvalidKeyException | CertificateException | NoSuchAlgorithmException
                    | NoSuchProviderException | SignatureException e) {
                System.out.println("\n"
                        + "\tSignature verification of certificate having distinguished name \n"
                        + "\t'" + certificate.getSubjectX500Principal() + "'\n"
                        + "\twith certificate having distinguished name (the issuer) \n"
                        + "\t'" + rootCertificate.getSubjectX500Principal() + "'\n"
                        + "\tfailed. Expected issuer has distinguished name \n"
                        + "\t'" + certificate.getIssuerX500Principal() + "' (" + e.getClass().getSimpleName() + ")");
            }
        }
        return false;
    }

    public static PdfFont loadFont(String fontName) {
        PdfFont pdfFont = null;
        try {
            pdfFont = PdfFontFactory.createFont(fontName);
        } catch (IOException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return pdfFont;
    }
}
