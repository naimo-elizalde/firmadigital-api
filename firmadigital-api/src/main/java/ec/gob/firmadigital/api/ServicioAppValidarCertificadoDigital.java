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
import ec.gob.firmadigital.libreria.certificate.CertEcUtils;
import ec.gob.firmadigital.libreria.certificate.to.DatosUsuario;
import ec.gob.firmadigital.libreria.exceptions.EntidadCertificadoraNoValidaException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST Web Service para validar certificados digitales.
 * Versión standalone usando directamente verificación local.
 *
 * @author Christian Espinosa, Misael Fernández
 */
@Path("/appvalidarcertificadodigital")
public class ServicioAppValidarCertificadoDigital extends RequestSizeFilter {

    private static final Logger LOGGER = Logger.getLogger(ServicioAppValidarCertificadoDigital.class.getName());
    private static final String WS_SYSTEM_PROPERTY = "firmadigital-servicio.url";
    private static final String REST_SERVICE_URL = System.getProperty(WS_SYSTEM_PROPERTY) + "/certificado";

    @POST
    @Secured
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String validarCertificado(
            @FormParam("pkcs12") String pkcs12Base64,
            @FormParam("password") String password
    ) {
        LOGGER.log(Level.INFO, "Iniciando validación de certificado digital");
        
        try {
            // Validar parámetros requeridos
            if (pkcs12Base64 == null || pkcs12Base64.isEmpty()) {
                return crearRespuestaError("El certificado PKCS#12 es requerido");
            }
            if (password == null || password.isEmpty()) {
                return crearRespuestaError("La contraseña del certificado es requerida");
            }
            
            // 1. Decodificar certificado PKCS#12
            LOGGER.log(Level.INFO, "Decodificando certificado PKCS#12");
            byte[] certBytes = decodificarBase64(pkcs12Base64);
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new ByteArrayInputStream(certBytes), password.toCharArray());
            
            // 2. Obtener certificado
            LOGGER.log(Level.INFO, "Obteniendo certificado X509");
            String alias = keyStore.aliases().nextElement();
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
            
            if (cert == null) {
                return crearRespuestaError("No se pudo obtener el certificado del archivo");
            }
            
            // 3. Verificar validez temporal
            boolean valido = true;
            String estado = "VIGENTE";
            String motivoNoValido = null;
            
            try {
                cert.checkValidity();
                LOGGER.log(Level.INFO, "Certificado vigente");
            } catch (CertificateExpiredException e) {
                valido = false;
                estado = "EXPIRADO";
                motivoNoValido = "El certificado ha expirado";
                LOGGER.log(Level.WARNING, "Certificado expirado");
            } catch (CertificateNotYetValidException e) {
                valido = false;
                estado = "NO_VIGENTE_AUN";
                motivoNoValido = "El certificado aún no es válido";
                LOGGER.log(Level.WARNING, "Certificado aún no vigente");
            }

            // 4. Verificar revocación en servicio CRL
            RevocacionInfo revocacionInfo = consultarRevocacion(cert.getSerialNumber());
            if (revocacionInfo.revocacionConsultada && revocacionInfo.revocado != null && revocacionInfo.revocado) {
                valido = false;
                if ("VIGENTE".equals(estado)) {
                    estado = "REVOCADO";
                    motivoNoValido = "El certificado está revocado";
                } else if (motivoNoValido != null && !motivoNoValido.contains("revocado")) {
                    motivoNoValido = motivoNoValido + ". Además, el certificado está revocado";
                }
            }
            
            // 5. Extraer información del certificado
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            
            // Extraer información del Subject
            String subject = cert.getSubjectDN().toString();
            String issuer = cert.getIssuerDN().toString();
            
            // Parsear el CN (Common Name) del subject
            String nombreTitular = extraerCN(subject);
            
            // 6. Construir respuesta
            JsonObject response = new JsonObject();
            response.addProperty("resultado", "OK");
            response.addProperty("valido", valido);
            response.addProperty("estado", estado);
            response.addProperty("subject", subject);
            response.addProperty("issuer", issuer);
            response.addProperty("nombreTitular", nombreTitular);
            response.addProperty("serialNumber", cert.getSerialNumber().toString());
            response.addProperty("notBefore", sdf.format(cert.getNotBefore()));
            response.addProperty("notAfter", sdf.format(cert.getNotAfter()));
            response.addProperty("version", cert.getVersion());
            response.addProperty("algoritmo", cert.getSigAlgName());
            response.addProperty("revocacionConsultada", revocacionInfo.revocacionConsultada);

            if (revocacionInfo.revocado != null) {
                response.addProperty("revocado", revocacionInfo.revocado);
            }

            if (revocacionInfo.fechaRevocado != null && !revocacionInfo.fechaRevocado.isBlank()) {
                response.addProperty("fechaRevocado", revocacionInfo.fechaRevocado);
            }

            if (revocacionInfo.detalleRevocacion != null && !revocacionInfo.detalleRevocacion.isBlank()) {
                response.addProperty("detalleRevocacion", revocacionInfo.detalleRevocacion);
            }
            
            if (motivoNoValido != null) {
                response.addProperty("motivoNoValido", motivoNoValido);
            }

            // 7. Extraer cédula/RUC y datos del titular
            try {
                DatosUsuario datosUsuario = CertEcUtils.getDatosUsuarios(cert);
                response.addProperty("cedula", datosUsuario.getCedula());
                response.addProperty("nombre", datosUsuario.getNombre());
                response.addProperty("apellido", datosUsuario.getApellido());
                response.addProperty("institucion", datosUsuario.getInstitucion());
                response.addProperty("cargo", datosUsuario.getCargo());
            } catch (EntidadCertificadoraNoValidaException e) {
                LOGGER.log(Level.WARNING, "No se pudo determinar la entidad certificadora: {0}", e.getMessage());
                response.addProperty("cedula", (String) null);
            }

            // Calcular días hasta expiración
            long diasHastaExpiracion = calcularDiasHastaExpiracion(cert.getNotAfter());
            response.addProperty("diasHastaExpiracion", diasHastaExpiracion);

            if (diasHastaExpiracion > 0 && diasHastaExpiracion <= 30) {
                response.addProperty("advertencia", "El certificado expirará en menos de 30 días");
            }
            
            LOGGER.log(Level.INFO, "Validación completada: {0}", estado);
            return new Gson().toJson(response);
            
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Error de formato en los datos: {0}", e.getMessage());
            return crearRespuestaError("Error de formato: El certificado no está correctamente codificado en Base64");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al validar certificado: {0}", e);
            return crearRespuestaError("Error al validar certificado: " + e.getMessage());
        }
    }

    private RevocacionInfo consultarRevocacion(BigInteger serial) {
        RevocacionInfo info = new RevocacionInfo();

        try {
            String baseUrl = System.getProperty(WS_SYSTEM_PROPERTY);
            if (baseUrl == null || baseUrl.isBlank()) {
                info.detalleRevocacion = "No se pudo consultar revocación: propiedad " + WS_SYSTEM_PROPERTY + " no configurada";
                return info;
            }

            try (Client client = ClientBuilder.newClient()) {
                WebTarget targetRevocado = client.target(REST_SERVICE_URL + "/revocado").path("{serial}")
                        .resolveTemplate("serial", serial);
                Invocation.Builder builderRevocado = targetRevocado.request();
                Invocation invocationRevocado = builderRevocado.buildGet();
                String revocadoResponse = invocationRevocado.invoke(String.class);

                info.revocacionConsultada = true;
                info.revocado = Boolean.parseBoolean(revocadoResponse != null ? revocadoResponse.trim() : "false");

                if (Boolean.TRUE.equals(info.revocado)) {
                    WebTarget targetFecha = client.target(REST_SERVICE_URL + "/fechaRevocado").path("{serial}")
                            .resolveTemplate("serial", serial);
                    Invocation.Builder builderFecha = targetFecha.request();
                    Invocation invocationFecha = builderFecha.buildGet();
                    String fechaResponse = invocationFecha.invoke(String.class);

                    if (fechaResponse != null) {
                        String fecha = fechaResponse.trim();
                        if (!fecha.isEmpty() && !"null".equalsIgnoreCase(fecha)) {
                            info.fechaRevocado = fecha;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No fue posible consultar revocación para serial {0}: {1}",
                    new Object[]{serial, e.getMessage()});
            info.detalleRevocacion = "No se pudo consultar revocación: " + e.getMessage();
        }

        return info;
    }

    private static class RevocacionInfo {

        private boolean revocacionConsultada;
        private Boolean revocado;
        private String fechaRevocado;
        private String detalleRevocacion;
    }
    
    /**
     * Extrae el Common Name (CN) del Distinguished Name
     */
    private String extraerCN(String dn) {
        if (dn == null) return "";
        
        String[] parts = dn.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.toUpperCase().startsWith("CN=")) {
                return part.substring(3);
            }
        }
        return "";
    }
    
    /**
     * Calcula los días hasta la expiración del certificado
     */
    private long calcularDiasHastaExpiracion(Date notAfter) {
        long diff = notAfter.getTime() - new Date().getTime();
        return diff / (1000 * 60 * 60 * 24);
    }
    
    private String crearRespuestaError(String mensaje) {
        JsonObject error = new JsonObject();
        error.addProperty("resultado", "ERROR");
        error.addProperty("mensaje", mensaje);
        error.addProperty("valido", false);
        return new Gson().toJson(error);
    }
    
    /**
     * Decodifica una cadena Base64 limpiando caracteres no válidos.
     * Elimina espacios, saltos de línea y otros caracteres no Base64.
     */
    private byte[] decodificarBase64(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            throw new IllegalArgumentException("Cadena Base64 vacía");
        }
        
        // Limpiar la cadena: eliminar espacios, saltos de línea, retornos de carro, tabulaciones
        String cleaned = base64String.trim().replaceAll("\\s+", "");
        
        try {
            // Intentar con decoder estándar primero
            return Base64.getDecoder().decode(cleaned);
        } catch (IllegalArgumentException e1) {
            LOGGER.log(Level.WARNING, "Fallo decodificación estándar, intentando con MIME decoder: {0}", e1.getMessage());
            
            try {
                // Intentar con MIME decoder que es más permisivo
                return Base64.getMimeDecoder().decode(cleaned);
            } catch (IllegalArgumentException e2) {
                LOGGER.log(Level.SEVERE, "Error al decodificar Base64 con ambos decoders: {0}", e2.getMessage());
                throw new IllegalArgumentException("El contenido Base64 no es válido. Verifique que el contenido esté correctamente codificado.");
            }
        }
    }
}
