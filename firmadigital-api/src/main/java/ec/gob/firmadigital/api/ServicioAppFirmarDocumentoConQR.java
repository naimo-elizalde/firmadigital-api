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

@Path("/appfirmardocumentoconqr")
public class ServicioAppFirmarDocumentoConQR extends RequestSizeFilter {

    private static final Logger LOGGER = Logger.getLogger(ServicioAppFirmarDocumentoConQR.class.getName());

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
        LOGGER.log(Level.INFO, "Iniciando firma con QR");

        try {
            if (pkcs12Base64 == null || pkcs12Base64.isEmpty()) {
                return crearRespuestaError("El certificado PKCS#12 es requerido");
            }
            if (password == null || password.isEmpty()) {
                return crearRespuestaError("La clave del certificado es requerida");
            }
            if (documentoBase64 == null || documentoBase64.isEmpty()) {
                return crearRespuestaError("El documento a firmar es requerido");
            }

            // 1. Cargar certificado PKCS#12
            byte[] certBytes = Base64Utils.decodificar(pkcs12Base64);
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new ByteArrayInputStream(certBytes), password.toCharArray());

            String alias = keyStore.aliases().nextElement();
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
            Certificate[] certChain = keyStore.getCertificateChain(alias);

            if (privateKey == null) {
                return crearRespuestaError("No se pudo obtener la llave privada del certificado");
            }
            if (certChain == null || certChain.length == 0) {
                return crearRespuestaError("No se pudo obtener la cadena de certificados");
            }

            // 2. Decodificar documento
            byte[] docBytes = Base64Utils.decodificar(documentoBase64);

            // 3. Configurar parametros de firma con QR
            Properties params = new Properties();
            params.setProperty("typeSignature", "QR");

            float qrPosX = 50f;
            float qrPosY = 50f;
            float qrAncho = 200f;
            float qrAlto = 50f;

            if (jsonMetadata != null && !jsonMetadata.isEmpty()) {
                try {
                    JsonObject metadata = new Gson().fromJson(jsonMetadata, JsonObject.class);

                    if (metadata.has("razon")) {
                        params.setProperty("signingReason", metadata.get("razon").getAsString());
                    }
                    if (metadata.has("localizacion")) {
                        params.setProperty("signingLocation", metadata.get("localizacion").getAsString());
                    }
                    if (metadata.has("cargo")) {
                        params.setProperty("cargo", metadata.get("cargo").getAsString());
                    }
                    if (metadata.has("identificacion")) {
                        params.setProperty("identificacion", metadata.get("identificacion").getAsString());
                    }
                    if (metadata.has("infoQR")) {
                        params.setProperty("infoQR", metadata.get("infoQR").getAsString());
                    }
                    if (metadata.has("qrPagina")) {
                        int qrPagina = metadata.get("qrPagina").getAsInt();
                        if (qrPagina > 0) {
                            params.setProperty("page", String.valueOf(qrPagina));
                        }
                    }
                    if (metadata.has("qrPosX")) qrPosX = metadata.get("qrPosX").getAsFloat();
                    if (metadata.has("qrPosY")) qrPosY = metadata.get("qrPosY").getAsFloat();
                    if (metadata.has("qrAncho")) qrAncho = metadata.get("qrAncho").getAsFloat();
                    if (metadata.has("qrAlto")) qrAlto = metadata.get("qrAlto").getAsFloat();

                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error al parsear metadatos JSON: {0}", e.getMessage());
                }
            }

            // 4. Extraer identificacion del certificado si no fue proporcionada
            if (params.getProperty("identificacion") == null || params.getProperty("identificacion").isEmpty()) {
                try {
                    X509Certificate x509Cert = (X509Certificate) certChain[0];
                    DatosUsuario datosUsuario = CertEcUtils.getDatosUsuarios(x509Cert);
                    if (datosUsuario != null && datosUsuario.getCedula() != null && !datosUsuario.getCedula().isEmpty()) {
                        params.setProperty("identificacion", datosUsuario.getCedula());
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "No se pudo extraer identificacion del certificado: {0}", e.getMessage());
                }
            }

            // Forzar tamaño mínimo para que la firma sea legible
            if (qrAncho < 150f) qrAncho = 150f;
            if (qrAlto < 50f) qrAlto = 50f;

            params.setProperty("PositionOnPageLowerLeftX", String.valueOf((int) qrPosX));
            params.setProperty("PositionOnPageLowerLeftY", String.valueOf((int) qrPosY));
            params.setProperty("PositionOnPageUpperRightX", String.valueOf((int) qrAncho));
            params.setProperty("PositionOnPageUpperRightY", String.valueOf((int) qrAlto));

            // 5. Firmar
            PrivateKeySigner signer = new PrivateKeySigner(privateKey, DigestAlgorithm.SHA256);
            PadesBasic padesSigner = new PadesBasic(signer);
            byte[] documentoFirmado = padesSigner.sign(new ByteArrayInputStream(docBytes), signer, certChain, params);

            // 6. Respuesta
            String documentoFirmadoBase64 = Base64.getEncoder().encodeToString(documentoFirmado);

            JsonObject response = new JsonObject();
            response.addProperty("resultado", "OK");
            response.addProperty("mensaje", "Documento firmado con QR exitosamente");
            response.addProperty("documentoFirmado", documentoFirmadoBase64);
            return new Gson().toJson(response);

        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Error de formato: {0}", e.getMessage());
            return crearRespuestaError("Error de formato: " + e.getMessage());
        } catch (java.security.UnrecoverableKeyException e) {
            LOGGER.log(Level.SEVERE, "Clave incorrecta: {0}", e.getMessage());
            return crearRespuestaError("La clave del certificado es incorrecta");
        } catch (java.io.IOException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("password") || msg.contains("mac check") || msg.contains("mac verify")) {
                return crearRespuestaError("La clave del certificado es incorrecta");
            }
            if (msg.contains("keystore") || msg.contains("pkcs12") || msg.contains("der")) {
                return crearRespuestaError("El archivo del certificado esta danado o no es un PKCS#12 valido");
            }
            LOGGER.log(Level.SEVERE, "Error de IO: {0}", e);
            return crearRespuestaError("Error al procesar el documento: " + msg);
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("TSA")) {
                return crearRespuestaError(msg);
            }
            LOGGER.log(Level.SEVERE, "Error al firmar: {0}", e);
            return crearRespuestaError("Error al firmar documento: " + msg);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inesperado: {0}", e);
            return crearRespuestaError("Error interno al firmar documento. Intente nuevamente");
        }
    }

    private String crearRespuestaError(String mensaje) {
        JsonObject error = new JsonObject();
        error.addProperty("resultado", "ERROR");
        error.addProperty("mensaje", mensaje);
        return new Gson().toJson(error);
    }
}
