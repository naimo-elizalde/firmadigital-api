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
import ec.gob.firmadigital.libreria.sign.DigestAlgorithm;
import ec.gob.firmadigital.libreria.sign.PrivateKeySigner;
import ec.gob.firmadigital.libreria.sign.pdf.PadesBasic;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST Web Service para firmar documentos PDF.
 * Versión standalone usando directamente la librería de firma digital.
 *
 * @author Christian Espinosa, Misael Fernández
 */
@Path("/appfirmardocumento")
public class ServicioAppFirmarDocumento extends RequestSizeFilter {

    private static final Logger LOGGER = Logger.getLogger(ServicioAppFirmarDocumento.class.getName());

    @POST
    @Secured
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String firmarDocumento(
            @FormParam("pkcs12") String pkcs12Base64,
            @FormParam("password") String password,
            @FormParam("documento") String documentoBase64,
            @FormParam("json") String jsonMetadata
    ) {
        LOGGER.log(Level.INFO, "Iniciando proceso de firma de documento");
        
        try {
            // Validar parámetros requeridos
            if (pkcs12Base64 == null || pkcs12Base64.isEmpty()) {
                return crearRespuestaError("El certificado PKCS#12 es requerido");
            }
            if (password == null || password.isEmpty()) {
                return crearRespuestaError("La contraseña del certificado es requerida");
            }
            if (documentoBase64 == null || documentoBase64.isEmpty()) {
                return crearRespuestaError("El documento a firmar es requerido");
            }
            
            // 1. Decodificar certificado PKCS#12
            LOGGER.log(Level.INFO, "Decodificando certificado PKCS#12");
            byte[] certBytes = decodificarBase64(pkcs12Base64);
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new ByteArrayInputStream(certBytes), password.toCharArray());
            
            // 2. Obtener llave privada y cadena de certificados
            LOGGER.log(Level.INFO, "Obteniendo llave privada y certificados");
            String alias = keyStore.aliases().nextElement();
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
            Certificate[] certChain = keyStore.getCertificateChain(alias);
            
            if (privateKey == null) {
                return crearRespuestaError("No se pudo obtener la llave privada del certificado");
            }
            if (certChain == null || certChain.length == 0) {
                return crearRespuestaError("No se pudo obtener la cadena de certificados");
            }
            
            // 3. Decodificar documento
            LOGGER.log(Level.INFO, "Decodificando documento PDF");
            byte[] docBytes = decodificarBase64(documentoBase64);
            
            // 4. Parsear metadatos (si existen)
            Properties params = new Properties();
            if (jsonMetadata != null && !jsonMetadata.isEmpty()) {
                try {
                    JsonObject metadata = new Gson().fromJson(jsonMetadata, JsonObject.class);
                    if (metadata.has("razon")) {
                        params.setProperty("razon", metadata.get("razon").getAsString());
                    }
                    if (metadata.has("localizacion")) {
                        params.setProperty("localizacion", metadata.get("localizacion").getAsString());
                    }
                    if (metadata.has("cargo")) {
                        params.setProperty("cargo", metadata.get("cargo").getAsString());
                    }
                    LOGGER.log(Level.INFO, "Metadatos de firma: {0}", metadata);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error al parsear metadatos JSON, se ignorarán: {0}", e.getMessage());
                }
            }
            
            // 5. Crear firmador y firmar documento
            LOGGER.log(Level.INFO, "Iniciando firma del documento");
            PrivateKeySigner signer = new PrivateKeySigner(privateKey, DigestAlgorithm.SHA256);
            PadesBasic padesSigner = new PadesBasic(signer);
            
            // Convertir byte[] a InputStream para el método sign
            ByteArrayInputStream inputStream = new ByteArrayInputStream(docBytes);
            byte[] documentoFirmado = padesSigner.sign(inputStream, signer, certChain, params);
            
            // 6. Codificar y retornar
            String documentoFirmadoBase64 = Base64.getEncoder().encodeToString(documentoFirmado);
            LOGGER.log(Level.INFO, "Documento firmado exitosamente");
            
            JsonObject response = new JsonObject();
            response.addProperty("resultado", "OK");
            response.addProperty("mensaje", "Documento firmado exitosamente");
            response.addProperty("documentoFirmado", documentoFirmadoBase64);
            
            return new Gson().toJson(response);
            
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Error de formato en los datos: {0}", e.getMessage());
            return crearRespuestaError("Error de formato: " + e.getMessage());
        } catch (java.security.UnrecoverableKeyException e) {
            LOGGER.log(Level.SEVERE, "Contraseña incorrecta: {0}", e.getMessage());
            return crearRespuestaError("Contraseña del certificado incorrecta");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al firmar documento: {0}", e);
            return crearRespuestaError("Error al firmar documento: " + e.getMessage());
        }
    }
    
    private String crearRespuestaError(String mensaje) {
        JsonObject error = new JsonObject();
        error.addProperty("resultado", "ERROR");
        error.addProperty("mensaje", mensaje);
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
        
        // Log de la longitud original para debugging
        LOGGER.log(Level.INFO, "Longitud Base64 original: {0}", base64String.length());
        
        // Limpiar la cadena: eliminar espacios, saltos de línea, retornos de carro, tabulaciones
        String cleaned = base64String.trim().replaceAll("\\s+", "");
        
        LOGGER.log(Level.INFO, "Longitud Base64 después de limpiar: {0}", cleaned.length());
        LOGGER.log(Level.FINE, "Primeros 50 caracteres: {0}", cleaned.substring(0, Math.min(50, cleaned.length())));
        LOGGER.log(Level.FINE, "Últimos 50 caracteres: {0}", cleaned.length() > 50 ? cleaned.substring(cleaned.length() - 50) : cleaned);
        
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
