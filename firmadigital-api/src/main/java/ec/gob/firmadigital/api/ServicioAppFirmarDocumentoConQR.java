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
            byte[] certBytes = Base64Utils.decodificar(pkcs12Base64);
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

            // 3. Extraer cédula del certificado X509 para TSA
            String identificacion = null;
            try {
                DatosUsuario datosUsuario = CertEcUtils.getDatosUsuarios((X509Certificate) certChain[0]);
                if (datosUsuario != null && datosUsuario.getCedula() != null) {
                    identificacion = datosUsuario.getCedula();
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "No se pudo extraer cédula del certificado: {0}", e.getMessage());
            }

            // 4. Decodificar documento
            LOGGER.log(Level.INFO, "Decodificando documento PDF");
            byte[] docBytes = Base64Utils.decodificar(documentoBase64);

            // 5. Parsear metadatos y configurar parámetros de firma con QR
            Properties params = new Properties();
            if (identificacion != null) {
                params.setProperty("identificacion", identificacion);
            }
            
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
                        params.setProperty("signingReason", metadata.get("razon").getAsString());
                    }
                    if (metadata.has("localizacion")) {
                        params.setProperty("signingLocation", metadata.get("localizacion").getAsString());
                    }
                    if (metadata.has("cargo")) {
                        params.setProperty("cargo", metadata.get("cargo").getAsString());
                    }
                    if (metadata.has("identificacion") && identificacion == null) {
                        params.setProperty("identificacion", metadata.get("identificacion").getAsString());
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
            params.setProperty("PositionOnPageLowerLeftX", String.valueOf((int) qrPosX));
            params.setProperty("PositionOnPageLowerLeftY", String.valueOf((int) qrPosY));
            params.setProperty("PositionOnPageUpperRightX", String.valueOf((int) qrAncho));
            params.setProperty("PositionOnPageUpperRightY", String.valueOf((int) qrAlto));

            // 6. Firmar digitalmente el documento con QR (la librería maneja todo)
            LOGGER.log(Level.INFO, "Iniciando firma digital del documento con QR");
            PrivateKeySigner signer = new PrivateKeySigner(privateKey, DigestAlgorithm.SHA256);
            PadesBasic padesSigner = new PadesBasic(signer);
            
            // Convertir byte[] a InputStream para el método sign
            ByteArrayInputStream inputStream = new ByteArrayInputStream(docBytes);
            byte[] documentoFirmado = padesSigner.sign(inputStream, signer, certChain, params);
            
            // 7. Codificar y retornar
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
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("TSA") || msg.contains("401") || msg.contains("409")) {
                LOGGER.log(Level.SEVERE, "Error TSA: {0}", msg);
                return crearRespuestaError("Error del servidor TSA: " + msg);
            }
            LOGGER.log(Level.SEVERE, "Error al procesar documento: {0}", e);
            return crearRespuestaError("Error al procesar documento: " + msg);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al procesar documento: {0}", e);
            return crearRespuestaError("Error al procesar documento: " + e.getMessage());
        }
    }

    private String crearRespuestaError(String mensaje) {
        JsonObject error = new JsonObject();
        error.addProperty("resultado", "ERROR");
        error.addProperty("mensaje", mensaje);
        return new Gson().toJson(error);
    }
    
}
