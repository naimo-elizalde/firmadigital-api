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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ec.gob.firmadigital.libreria.sign.SignInfo;
import ec.gob.firmadigital.libreria.sign.pdf.PadesSigner;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST Web Service para verificar documentos firmados.
 * Versión standalone usando directamente la librería de firma digital.
 *
 * @author Christian Espinosa, Misael Fernández
 */
@Path("/appverificardocumento")
public class ServicioAppVerificarDocumento extends RequestSizeFilter {

    private static final Logger LOGGER = Logger.getLogger(ServicioAppVerificarDocumento.class.getName());

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String verificarDocumento(
            @FormParam("jwt") String jwt,
            @FormParam("documento") String documentoBase64,
            @FormParam("base64") String base64Version
    ) {
        LOGGER.log(Level.INFO, "Iniciando verificación de documento firmado");
        
        try {
            // Validar parámetros requeridos
            if (documentoBase64 == null || documentoBase64.isEmpty()) {
                return crearRespuestaError("El documento es requerido");
            }
            
            // 1. Decodificar documento
            LOGGER.log(Level.INFO, "Decodificando documento");
            byte[] docBytes = Base64.getDecoder().decode(documentoBase64);
            
            // 2. Verificar firmas del PDF
            LOGGER.log(Level.INFO, "Extrayendo firmas del documento PDF");
            PadesSigner padesSigner = new PadesSigner();
            List<SignInfo> firmas = padesSigner.getSigners(docBytes);
            
            if (firmas == null || firmas.isEmpty()) {
                LOGGER.log(Level.WARNING, "No se encontraron firmas en el documento");
                JsonObject response = new JsonObject();
                response.addProperty("resultado", "OK");
                response.addProperty("firmaValida", false);
                response.addProperty("numeroFirmas", 0);
                response.addProperty("mensaje", "El documento no contiene firmas digitales");
                return new Gson().toJson(response);
            }
            
            // 3. Construir respuesta con información de cada firma
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            
            JsonObject response = new JsonObject();
            response.addProperty("resultado", "OK");
            response.addProperty("firmaValida", true);
            response.addProperty("numeroFirmas", firmas.size());
            
            JsonArray firmasArray = new JsonArray();
            int firmaIndex = 1;
            
            for (SignInfo signInfo : firmas) {
                try {
                    // getCerts() retorna un array, el primer elemento es el certificado del firmante
                    X509Certificate[] certs = signInfo.getCerts();
                    X509Certificate cert = certs[0];
                    
                    JsonObject firmaObj = new JsonObject();
                    firmaObj.addProperty("numeroFirma", firmaIndex++);
                    firmaObj.addProperty("firmante", cert.getSubjectDN().toString());
                    firmaObj.addProperty("nombreFirmante", extraerCN(cert.getSubjectDN().toString()));
                    firmaObj.addProperty("fechaFirma", sdf.format(signInfo.getSigningTime()));
                    firmaObj.addProperty("emisor", cert.getIssuerDN().toString());
                    firmaObj.addProperty("serialNumber", cert.getSerialNumber().toString());
                    
                    // Verificar validez del certificado al momento de la firma
                    boolean certValido = true;
                    String estadoCertificado = "VALIDO";
                    try {
                        cert.checkValidity(signInfo.getSigningTime());
                    } catch (Exception e) {
                        certValido = false;
                        estadoCertificado = "NO_VALIDO";
                        LOGGER.log(Level.WARNING, "Certificado no era válido al momento de la firma");
                    }
                    
                    firmaObj.addProperty("certificadoValido", certValido);
                    firmaObj.addProperty("estadoCertificado", estadoCertificado);
                    
                    // Información adicional del certificado
                    firmaObj.addProperty("validezDesde", sdf.format(cert.getNotBefore()));
                    firmaObj.addProperty("validezHasta", sdf.format(cert.getNotAfter()));
                    
                    firmasArray.add(firmaObj);
                    
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error al procesar firma {0}: {1}", 
                              new Object[]{firmaIndex, e.getMessage()});
                    
                    JsonObject firmaObj = new JsonObject();
                    firmaObj.addProperty("numeroFirma", firmaIndex++);
                    firmaObj.addProperty("error", "Error al procesar firma: " + e.getMessage());
                    firmasArray.add(firmaObj);
                }
            }
            
            response.add("firmas", firmasArray);
            
            LOGGER.log(Level.INFO, "Verificación completada. Se encontraron {0} firma(s)", firmas.size());
            return new Gson().toJson(response);
            
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Error de formato en los datos: {0}", e.getMessage());
            return crearRespuestaError("Error de formato: El documento no está correctamente codificado en Base64");
        } catch (ec.gob.firmadigital.libreria.exceptions.InvalidFormatException e) {
            LOGGER.log(Level.SEVERE, "Formato de documento inválido: {0}", e.getMessage());
            return crearRespuestaError("El documento no es un PDF válido o no contiene firmas reconocibles");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al verificar documento: {0}", e);
            return crearRespuestaError("Error al verificar documento: " + e.getMessage());
        }
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
    
    private String crearRespuestaError(String mensaje) {
        JsonObject error = new JsonObject();
        error.addProperty("resultado", "ERROR");
        error.addProperty("mensaje", mensaje);
        error.addProperty("firmaValida", false);
        return new Gson().toJson(error);
    }
}
