/*
 * Firma Digital: API
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ec.gob.firmadigital.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import ec.gob.firmadigital.api.security.Secured;
import ec.gob.firmadigital.api.utils.Base64Utils;
import ec.gob.firmadigital.libreria.certificate.CertEcUtils;
import ec.gob.firmadigital.libreria.certificate.to.DatosUsuario;
import ec.gob.firmadigital.libreria.crl.ServicioCRL;
import ec.gob.firmadigital.libreria.utils.CertificateUtils;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CRLReason;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/appvalidarcertificadodigital")
public class ServicioAppValidarCertificadoDigital extends RequestSizeFilter {

    private static final Logger LOGGER = Logger.getLogger(ServicioAppValidarCertificadoDigital.class.getName());
    private static final DateTimeFormatter ISO8601 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private static String fmtDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).format(ISO8601);
    }

    @POST
    @Secured
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String validarCertificado(
            @FormParam("pkcs12") String pkcs12Base64,
            @FormParam("password") String password
    ) {
        LOGGER.log(Level.INFO, "Iniciando validacion de certificado digital");

        try {
            if (pkcs12Base64 == null || pkcs12Base64.isEmpty()) {
                return crearRespuestaError("El certificado PKCS#12 es requerido");
            }
            if (password == null || password.isEmpty()) {
                return crearRespuestaError("La clave del certificado es requerida");
            }

            // 1. Cargar certificado PKCS#12
            byte[] certBytes = Base64Utils.decodificar(pkcs12Base64);
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new ByteArrayInputStream(certBytes), password.toCharArray());

            String alias = keyStore.aliases().nextElement();
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);

            if (cert == null) {
                return crearRespuestaError("No se pudo obtener el certificado del archivo");
            }

            // 2. Verificar validez temporal
            boolean vigente = true;
            String estado = "VIGENTE";
            String motivoNoValido = null;

            try {
                cert.checkValidity();
            } catch (CertificateExpiredException e) {
                vigente = false;
                estado = "EXPIRADO";
                motivoNoValido = "El certificado ha expirado";
            } catch (CertificateNotYetValidException e) {
                vigente = false;
                estado = "NO_VIGENTE_AUN";
                motivoNoValido = "El certificado aun no es valido";
            }

            // 3. Verificar revocacion CRL
            RevocacionInfo revocacionInfo = consultarRevocacion(cert);
            if (revocacionInfo.revocacionConsultada && revocacionInfo.revocado != null && revocacionInfo.revocado) {
                vigente = false;
                if ("VIGENTE".equals(estado)) {
                    estado = "REVOCADO";
                    motivoNoValido = "El certificado esta revocado";
                } else {
                    motivoNoValido = motivoNoValido + ". Ademas, el certificado esta revocado";
                }
            }

            // 4. Construir respuesta
            JsonObject response = new JsonObject();
            response.addProperty("resultado", "OK");
            response.addProperty("vigente", vigente);
            response.addProperty("estado", estado);

            // Datos del certificado
            response.addProperty("serial", cert.getSerialNumber().toString());
            response.addProperty("entidadCertificadora", CertEcUtils.getNombreCA(cert));
            response.addProperty("fechaEmision", fmtDate(cert.getNotBefore()));
            response.addProperty("fechaExpiracion", fmtDate(cert.getNotAfter()));
            response.addProperty("algoritmo", cert.getSigAlgName());
            response.addProperty("subject", cert.getSubjectDN().getName());
            response.addProperty("issuer", cert.getIssuerDN().getName());

            // Dias hasta expiracion
            long diasHastaExpiracion = calcularDiasHastaExpiracion(cert.getNotAfter());
            response.addProperty("diasHastaExpiracion", diasHastaExpiracion);
            if (diasHastaExpiracion > 0 && diasHastaExpiracion <= 30) {
                response.addProperty("advertencia", "El certificado expirara en menos de 30 dias");
            }

            // Revocacion
            JsonObject revObj = new JsonObject();
            revObj.addProperty("consultada", revocacionInfo.revocacionConsultada);
            if (revocacionInfo.revocado != null) {
                revObj.addProperty("revocado", revocacionInfo.revocado);
            }
            if (revocacionInfo.fechaRevocado != null) {
                revObj.addProperty("fecha", revocacionInfo.fechaRevocado);
            }
            if (revocacionInfo.detalleRevocacion != null) {
                revObj.addProperty("detalle", revocacionInfo.detalleRevocacion);
            }
            response.add("revocacion", revObj);

            if (motivoNoValido != null) {
                response.addProperty("motivoNoValido", motivoNoValido);
            }

            // 5. Datos del titular
            try {
                DatosUsuario datosUsuario = CertEcUtils.getDatosUsuarios(cert);
                if (datosUsuario != null) {
                    JsonObject d = new JsonObject();
                    d.addProperty("cedula", datosUsuario.getCedula());
                    d.addProperty("nombre", datosUsuario.getNombre());
                    d.addProperty("apellido", datosUsuario.getApellido());
                    d.addProperty("institucion", datosUsuario.getInstitucion());
                    d.addProperty("cargo", datosUsuario.getCargo());
                    d.addProperty("tipoCertificado", datosUsuario.getTipoCertificado());
                    d.addProperty("certificadoDigitalValido", datosUsuario.isCertificadoDigitalValido());
                    response.add("datosUsuario", d);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "No se pudo extraer datos del titular: {0}", e.getMessage());
            }

            return new Gson().toJson(response);

        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Error de formato: {0}", e.getMessage());
            return crearRespuestaError("El certificado no esta correctamente codificado en Base64");
        } catch (java.io.IOException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("password") || msg.contains("mac check") || msg.contains("mac verify")) {
                return crearRespuestaError("La clave del certificado es incorrecta");
            }
            if (msg.contains("keystore") || msg.contains("pkcs12") || msg.contains("der")) {
                return crearRespuestaError("El archivo del certificado esta danado o no es un PKCS#12 valido");
            }
            LOGGER.log(Level.SEVERE, "Error de IO: {0}", e);
            return crearRespuestaError("Error al procesar el certificado");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al validar certificado: {0}", e);
            return crearRespuestaError("Error interno al validar certificado. Intente nuevamente");
        }
    }

    private RevocacionInfo consultarRevocacion(X509Certificate cert) {
        RevocacionInfo info = new RevocacionInfo();

        try {
            List<String> crlUrls = new ArrayList<>();
            try {
                List<String> urlsDelCert = CertificateUtils.getCrlDistributionPoints(cert);
                if (urlsDelCert != null) {
                    crlUrls.addAll(urlsDelCert);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "No se pudieron extraer URLs CRL del certificado: {0}", e.getMessage());
            }

            String issuerDN = cert.getIssuerDN().toString().toLowerCase();
            String fallbackUrl = resolverFallbackCrl(issuerDN);
            if (fallbackUrl != null && !crlUrls.contains(fallbackUrl)) {
                crlUrls.add(fallbackUrl);
            }

            if (crlUrls.isEmpty()) {
                info.detalleRevocacion = "No se encontraron URLs de CRL en el certificado";
                return info;
            }

            BigInteger serial = cert.getSerialNumber();

            for (String crlUrl : crlUrls) {
                if (crlUrl == null || crlUrl.isBlank()) continue;

                try {
                    X509CRL crl = ServicioCRL.downloadCrl(crlUrl);
                    X509CRLEntry entry = crl.getRevokedCertificate(serial);

                    if (entry != null) {
                        info.revocacionConsultada = true;
                        info.revocado = true;
                        Date fechaRevocacion = entry.getRevocationDate();
                        if (fechaRevocacion != null) {
                            info.fechaRevocado = fmtDate(fechaRevocacion);
                        }
                        CRLReason razon = entry.getRevocationReason();
                        info.detalleRevocacion = razon != null ? razon.name() : "Revocado";
                        return info;
                    }

                    info.revocacionConsultada = true;
                    info.revocado = false;
                    info.detalleRevocacion = "No revocado";
                    return info;

                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "No fue posible consultar CRL en {0}: {1}",
                            new Object[]{crlUrl, e.getMessage()});
                }
            }

            info.detalleRevocacion = "No se pudo conectar al servidor CRL";
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error consultando revocacion: {0}", e.getMessage());
            info.detalleRevocacion = "Error al consultar revocacion";
        }

        return info;
    }

    private String resolverFallbackCrl(String issuerDN) {
        if (issuerDN.contains("lazzate")) {
            if (issuerDN.contains("ca2")) return ServicioCRL.LAZZATECA2_CRL;
            if (issuerDN.contains("ca1")) return ServicioCRL.LAZZATECA1_CRL;
            return ServicioCRL.LAZZATE_CRL;
        }
        if (issuerDN.contains("security data")) return ServicioCRL.SD_CRL1;
        if (issuerDN.contains("bce") || issuerDN.contains("banco central")) return ServicioCRL.BCE_CRL;
        if (issuerDN.contains("registro civil") || issuerDN.contains("digercic")) return ServicioCRL.DIGERCIC_CRL;
        if (issuerDN.contains("consejo de la judicatura") || issuerDN.contains("icert")) return ServicioCRL.CJ_CRL;
        if (issuerDN.contains("anf")) return ServicioCRL.ANFAC_CRL1;
        if (issuerDN.contains("uanataca")) return ServicioCRL.UANATACA_CRL1;
        if (issuerDN.contains("datil")) return ServicioCRL.DATIL_CRL;
        if (issuerDN.contains("argosdata")) return ServicioCRL.ARGOSDATA_CRL;
        if (issuerDN.contains("we-go") || issuerDN.contains("we go")) return ServicioCRL.LAZZATE_WE_GO_CRL;
        if (issuerDN.contains("corpnewbest") || issuerDN.contains("newbest")) return ServicioCRL.CORPNEWBEST_CRL1;
        if (issuerDN.contains("firmasegura")) return ServicioCRL.FIRMASEGURA_CRL;
        if (issuerDN.contains("letmi")) return ServicioCRL.LETMI2_CRL;
        if (issuerDN.contains("appfirmas") || issuerDN.contains("app firmas")) return ServicioCRL.APP_FIRMAS_CRL;
        if (issuerDN.contains("alpha") || issuerDN.contains("globalsign")) return ServicioCRL.ALPHATECHNOLOGIES_CA2_CRL;
        return null;
    }

    private static class RevocacionInfo {
        boolean revocacionConsultada;
        Boolean revocado;
        String fechaRevocado;
        String detalleRevocacion;
    }

    private long calcularDiasHastaExpiracion(Date notAfter) {
        long diff = notAfter.getTime() - new Date().getTime();
        return diff / (1000 * 60 * 60 * 24);
    }

    private String crearRespuestaError(String mensaje) {
        JsonObject error = new JsonObject();
        error.addProperty("resultado", "ERROR");
        error.addProperty("mensaje", mensaje);
        error.addProperty("vigente", false);
        return new Gson().toJson(error);
    }
}
