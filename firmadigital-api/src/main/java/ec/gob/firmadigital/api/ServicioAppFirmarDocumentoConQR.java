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
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST Web Service para firmar documentos PDF con estampado QR.
 * Este servicio usa la librería de firma digital con soporte nativo de QR
 * a través del parámetro typeSig="QR" y los parámetros infoQR y de posición.
 *
 * @author Luis González
 */
@Path("/appfirmardocumentoconqr")
public class ServicioAppFirmarDocumentoConQR extends RequestSizeFilter {

    private static final Logger LOGGER = Logger.getLogger(ServicioAppFirmarDocumentoConQR.class.getName());

    /**
     * Firma un documento PDF con estampado visual QR usando la librería.
     * 
     * @param pkcs12Base64 Certificado PKCS#12 codificado en Base64
     * @param password Contraseña del certificado
     * @param documentoBase64 Documento PDF codificado en Base64
     * @param jsonMetadata JSON con metadatos:
     *   Metadatos de firma:
     *   - razon: Razón de la firma (opcional, default: "Firma digital")
     *   - localizacion: Ubicación de la firma (opcional, default: "Ecuador")
     *   - cargo: Cargo del firmante (opcional)
     *   
     *   Metadatos del estampado QR:
     *   - infoQR: Información adicional para el QR (opcional, ej: URL de verificación)
     *   - qrPagina: Número de página donde estampar (opcional, default: última página)
     *   - qrPosX: Posición X del estampado (opcional, default: 50)
     *   - qrPosY: Posición Y del estampado (opcional, default: 50)
     *   - qrAncho: Ancho del estampado (opcional, default: 200)
     *   - qrAlto: Alto del estampado (opcional, default: 100)
     * @return JSON con el documento firmado y estampado en Base64
     */
    @POST
    @Secured
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String firmarDocumentoConQR(
            @FormParam("pkcs12") String pkcs12Base64,
            @FormParam("password") String password,
            @FormParam("documento") String documentoBase64,
            @FormParam("json") String jsonMetadata
    ) {
        LOGGER.log(Level.INFO, "Iniciando proceso de firma de documento con estampado QR");
        
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
            
            // Extraer nombre del firmante del certificado
            String nombreFirmante = extraerNombreFirmante(certChain[0]);
            
            // 3. Decodificar documento
            LOGGER.log(Level.INFO, "Decodificando documento PDF");
            byte[] docBytes = decodificarBase64(documentoBase64);
            
            // 4. Parsear metadatos y configurar parámetros de firma con QR
            Properties params = new Properties();
            
            // Configurar firma con QR
            params.setProperty("typeSignature", "QR");
            
            // Valores por defecto para posición y tamaño del QR
            float qrPosX = 50f;
            float qrPosY = 50f;
            float qrAncho = 200f;
            float qrAlto = 100f;
            
            if (jsonMetadata != null && !jsonMetadata.isEmpty()) {
                try {
                    JsonObject metadata = new Gson().fromJson(jsonMetadata, JsonObject.class);
                    
                    // Metadatos de firma
                    if (metadata.has("razon")) {
                        params.setProperty("razon", metadata.get("razon").getAsString());
                    }
                    if (metadata.has("localizacion")) {
                        params.setProperty("localizacion", metadata.get("localizacion").getAsString());
                    }
                    if (metadata.has("cargo")) {
                        params.setProperty("cargo", metadata.get("cargo").getAsString());
                    }
                    
                    // Metadatos del QR
                    if (metadata.has("infoQR")) {
                        params.setProperty("infoQR", metadata.get("infoQR").getAsString());
                    }
                    
                    // Posición y tamaño del QR
                    if (metadata.has("qrPagina")) {
                        int qrPagina = metadata.get("qrPagina").getAsInt();
                        if (qrPagina > 0) {
                            params.setProperty("page", String.valueOf(qrPagina));
                        }
                    }
                    
                    // Leer posición y tamaño personalizados
                    if (metadata.has("qrPosX")) {
                        qrPosX = metadata.get("qrPosX").getAsFloat();
                    }
                    if (metadata.has("qrPosY")) {
                        qrPosY = metadata.get("qrPosY").getAsFloat();
                    }
                    if (metadata.has("qrAncho")) {
                        qrAncho = metadata.get("qrAncho").getAsFloat();
                    }
                    if (metadata.has("qrAlto")) {
                        qrAlto = metadata.get("qrAlto").getAsFloat();
                    }
                    
                    LOGGER.log(Level.INFO, "Metadatos procesados: {0}", metadata);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error al parsear metadatos JSON, se usarán valores por defecto: {0}", e.getMessage());
                }
            }
            
            // Configurar posición del QR usando los nombres correctos de la librería
            params.setProperty("PositionOnPageLowerLeftX", String.valueOf((int)qrPosX));
            params.setProperty("PositionOnPageLowerLeftY", String.valueOf((int)qrPosY));
            params.setProperty("PositionOnPageUpperRightX", String.valueOf((int)qrAncho));
            params.setProperty("PositionOnPageUpperRightY", String.valueOf((int)qrAlto));
            
            // 5. Firmar digitalmente el documento con QR (la librería maneja todo)
            LOGGER.log(Level.INFO, "Iniciando firma digital del documento con QR");
            PrivateKeySigner signer = new PrivateKeySigner(privateKey, DigestAlgorithm.SHA256);
            PadesBasic padesSigner = new PadesBasic(signer);
            
            // Convertir byte[] a InputStream para el método sign
            ByteArrayInputStream inputStream = new ByteArrayInputStream(docBytes);
            byte[] documentoFirmado = padesSigner.sign(inputStream, signer, certChain, params);
            
            // 6. Codificar y retornar
            String documentoFirmadoBase64 = Base64.getEncoder().encodeToString(documentoFirmado);
            LOGGER.log(Level.INFO, "Documento firmado con QR exitosamente");
            
            JsonObject response = new JsonObject();
            response.addProperty("resultado", "OK");
            response.addProperty("mensaje", "Documento firmado con QR exitosamente");
            response.addProperty("documentoFirmado", documentoFirmadoBase64);
            
            return new Gson().toJson(response);
            
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Error de formato en los datos: {0}", e.getMessage());
            return crearRespuestaError("Error de formato: " + e.getMessage());
        } catch (java.security.UnrecoverableKeyException e) {
            LOGGER.log(Level.SEVERE, "Contraseña incorrecta: {0}", e.getMessage());
            return crearRespuestaError("Contraseña del certificado incorrecta");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al procesar documento: {0}", e);
            return crearRespuestaError("Error al procesar documento: " + e.getMessage());
        }
    }
    
    /**
     * Extrae el nombre común (CN) del certificado.
     */
    private String extraerNombreFirmante(Certificate cert) {
        try {
            if (cert instanceof X509Certificate) {
                X509Certificate x509cert = (X509Certificate) cert;
                String dn = x509cert.getSubjectDN().getName();
                
                // Buscar CN= en el DN
                String[] parts = dn.split(",");
                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("CN=")) {
                        return part.substring(3).trim();
                    }
                }
                
                // Si no encuentra CN, retornar el DN completo
                return dn;
            }
            return "Firmante desconocido";
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al extraer nombre del firmante: {0}", e.getMessage());
            return "Firmante desconocido";
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
