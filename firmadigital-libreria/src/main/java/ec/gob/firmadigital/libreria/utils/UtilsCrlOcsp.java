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

import ec.gob.firmadigital.libreria.exceptions.CRLValidationException;
import ec.gob.firmadigital.libreria.exceptions.CertificadoInvalidoException;
import ec.gob.firmadigital.libreria.exceptions.ConexionValidarCRLException;
import ec.gob.firmadigital.libreria.certificate.CertEcUtils;
import ec.gob.firmadigital.libreria.certificate.CrlUtils;
import ec.gob.firmadigital.libreria.certificate.ValidationResult;
import ec.gob.firmadigital.libreria.exceptions.RubricaException;
import ec.gob.firmadigital.libreria.exceptions.ConexionApiException;
import ec.gob.firmadigital.libreria.exceptions.ConexionException;
import ec.gob.firmadigital.libreria.exceptions.EntidadCertificadoraNoValidaException;

import ec.gob.firmadigital.libreria.ocsp.ValidadorOCSP;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilidades para CRL y OCSP
 *
 * @author jdc
 */
public class UtilsCrlOcsp {

    private static final int TIME_OUT = 5000; //set timeout to 5 seconds
    private static final Logger LOGGER = Logger.getLogger(UtilsCrlOcsp.class.getName());

    public UtilsCrlOcsp() {
    }

    /**
     * Valida primero por OCSP, si falla lo hace por CRL
     *
     * @param cert
     * @param apiUrl
     * @return X509Certificate
     * @throws IOException
     * @throws RubricaException si hay un error de conexion con el CRL bota
     * esto, si es por OCSP y falla la conexion intenta por CRL
     * @throws ec.gob.firmadigital.libreria.exceptions.CRLValidationException
     * @throws
     * ec.gob.firmadigital.libreria.exceptions.EntidadCertificadoraNoValidaException
     * @throws
     * ec.gob.firmadigital.libreria.exceptions.ConexionValidarCRLException
     */
    public static String validarCertificado(X509Certificate cert, String apiUrl) throws EntidadCertificadoraNoValidaException, IOException, RubricaException, ConexionValidarCRLException, CRLValidationException {
        String fechaRevocado = null;
        try {
            BigInteger serial = cert.getSerialNumber();
            fechaRevocado = validarCrlServidorAPI(serial, apiUrl);
        } catch (UnknownHostException | SocketException | ConexionApiException ex) {
            System.out.println("Fallo la validacion por el servicio del API");
            LOGGER.log(Level.SEVERE, "SocketException: ", ex.getCause());
            fechaRevocado = "errorRed";
//            try {
//                System.out.println("Fallo la validacion por el servicio del API, Ahora intentamos por CRL");
//                fechaRevocado = validarCRL(cert);
//            } catch (IOException | RubricaException ex1) {
//                System.out.println("Fallo la validacion por OCSP, Ahora intentamos por CRL");
//                fechaRevocado = validarOCSP(cert);
//                if (fechaRevocado.equals("unknownStatus")) {
//                    System.out.println("Fallo la validacion por OCSP");
//                    fechaRevocado = null;
//                }
//            }
        } finally {
            return fechaRevocado;
        }
    }

    public static Date fechaString_Date(String fecha) {
        DateFormat formato = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        if (fecha != null) {
            try {
                date = (Date) formato.parse(fecha);
            } catch (ParseException ex) {
                LOGGER.log(Level.SEVERE, fecha, ex);
            }
        }
        return date;
    }

    public static Date validarFechaRevocado(X509Certificate cert, String apiUrl) throws CertificadoInvalidoException, IOException, ConexionException {
        Date fechaRevocado = null;
        try {
            String revocado = validarCertificado(cert, apiUrl);
            if (revocado != null && revocado.contains("errorRed")) {
                throw new ConexionException("Problemas en la red, no es posible conectarse");
            } else {
                fechaRevocado = fechaString_Date(revocado);
            }
        } catch (RubricaException | ConexionValidarCRLException | CRLValidationException | EntidadCertificadoraNoValidaException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
//            throw new ConexionFirmadorApiException("Fallo la validacion por el servicio del API");
        }
        return fechaRevocado;
    }

    public static Date validarOCSPDate(X509Certificate cert) throws IOException, RubricaException, EntidadCertificadoraNoValidaException {
        List<String> ocspUrls = CertificateUtils.getAuthorityInformationAccess(cert);
        ocspUrls.forEach((ocsp) -> {
            System.out.println("OCSP=" + ocsp);
        });

        X509Certificate certRoot = CertEcUtils.getRootCertificate(cert);

        Date fechaRevocado = null;
        try {
            fechaRevocado = fechaString_Date(ValidadorOCSP.ValidarOCSP(cert, certRoot, ocspUrls.get(0)));
        } catch (RubricaException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return fechaRevocado;
    }

    public static String validarOCSP(X509Certificate cert) throws IOException, RubricaException, EntidadCertificadoraNoValidaException {
        List<String> ocspUrls = CertificateUtils.getAuthorityInformationAccess(cert);
        ocspUrls.forEach((ocsp) -> {
            System.out.println("OCSP=" + ocsp);
        });

        X509Certificate certRoot = CertEcUtils.getRootCertificate(cert);
        return ValidadorOCSP.ValidarOCSP(cert, certRoot, ocspUrls.get(0));
    }

    public static String validarCRL(X509Certificate cert) throws IOException, EntidadCertificadoraNoValidaException, RubricaException, ConexionValidarCRLException, CRLValidationException {
        X509Certificate root = CertEcUtils.getRootCertificate(cert);
        CrlUtils crlUtils = new CrlUtils();
        String urlCrl = obtenerUrlCRL(CertificateUtils.getCrlDistributionPoints(cert));
        ValidationResult result = CrlUtils.verifyCertificateCRLs(cert, root.getPublicKey(), Arrays.asList(urlCrl));
        if (result == result.CANNOT_DOWNLOAD_CRL) {
            throw new ConexionValidarCRLException("No se puede validar contra la lista de revocación:" + urlCrl);
        }
        // Si el certificado no es valido lanzamos exception
        if (!result.isValid()) {
            throw new CRLValidationException("Certificado Inválido");
        }
        return crlUtils.getRevocationDate();
    }

    private static String validarCrlServidorAPI(BigInteger serial, String apiUrl) throws IOException, ConexionApiException {
        String certificado_revocado_url = apiUrl == null ? PropertiesUtils.getConfig().getProperty("certificado_revocado_url") : apiUrl;
        if (!certificado_revocado_url.isEmpty()) {
            URL url = new URL(certificado_revocado_url + "/" + serial);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            HttpURLConnection originalConnection = urlConnection;
            try {
                int responseCode = urlConnection.getResponseCode();

                if (responseCode >= 300 && responseCode < 400) {
                    HttpURLConnection redirectConnection = (HttpURLConnection) new URL(urlConnection.getHeaderField("Location")).openConnection();
                    originalConnection.disconnect();
                    originalConnection = null;
                    urlConnection = redirectConnection;
                    urlConnection.setConnectTimeout(TIME_OUT);
                    responseCode = urlConnection.getResponseCode();
                }
                if (responseCode >= 400) {
                    LOGGER.log(Level.SEVERE, "{0}/{1}: Response Code: {2}", new Object[]{certificado_revocado_url, serial, responseCode});
                    throw new ConexionApiException("No se pudo conectar API. " + certificado_revocado_url + " Response Code: " + responseCode);
                }

                try (InputStream is = urlConnection.getInputStream();
                     BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
                    return in.readLine();
                }
            } finally {
                urlConnection.disconnect();
                if (originalConnection != null) {
                    originalConnection.disconnect();
                }
            }
        } else {
            return null;
        }
    }

    private static String obtenerUrlCRL(List<String> urls) {
        for (String url : urls) {
            if (url.toLowerCase().contains("crl")) {
                return url;
            }
        }
        return null;
    }

    private static String resultadosCRL(ValidationResult result) {
        if (result == result.CANNOT_DOWNLOAD_CRL) {
            return "No se pudo descargar el archivo CRL\nRevisar conexión de Internet";
        }
        if (result.isValid()) {
            return "Válido";
        }
        return "Inválido";
    }
}
