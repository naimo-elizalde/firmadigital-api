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
import com.itextpdf.kernel.pdf.PdfReader;
import ec.gob.firmadigital.api.security.Secured;
import ec.gob.firmadigital.api.utils.Base64Utils;
import ec.gob.firmadigital.libreria.certificate.to.Certificado;
import ec.gob.firmadigital.libreria.certificate.to.DatosUsuario;
import ec.gob.firmadigital.libreria.certificate.to.Documento;
import ec.gob.firmadigital.libreria.sign.SignInfo;
import ec.gob.firmadigital.libreria.sign.pdf.PadesSigner;
import ec.gob.firmadigital.libreria.utils.Utils;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST Web Service para verificar documentos firmados.
 *
 * @author Christian Espinosa, Misael Fernández
 */
@Path("/appverificardocumento")
public class ServicioAppVerificarDocumento extends RequestSizeFilter {

    private static final Logger LOGGER = Logger.getLogger(ServicioAppVerificarDocumento.class.getName());
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @POST
    @Secured
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String verificarDocumento(
            @FormParam("documento") String documentoBase64
    ) {
        LOGGER.log(Level.INFO, "Iniciando verificación de documento firmado");

        try {
            if (documentoBase64 == null || documentoBase64.isEmpty()) {
                return crearRespuestaError("El documento es requerido");
            }

            byte[] docBytes = Base64Utils.decodificar(documentoBase64);

            PadesSigner padesSigner = new PadesSigner();
            List<SignInfo> signInfos = padesSigner.getSigners(docBytes);

            PdfReader pdfReader = new PdfReader(new ByteArrayInputStream(docBytes));
            Documento documento = Utils.pdfToDocumento(pdfReader, signInfos);

            List<Certificado> certificados = documento.getCertificados();

            JsonObject response = new JsonObject();
            response.addProperty("resultado", "OK");
            response.addProperty("firmasValidas", documento.getSignValidate());
            response.addProperty("documentoIntegro", documento.getDocValidate());
            response.addProperty("numeroFirmas", certificados != null ? certificados.size() : 0);

            JsonArray firmasArray = new JsonArray();
            if (certificados != null) {
                for (Certificado cert : certificados) {
                    JsonObject firmaObj = new JsonObject();
                    firmaObj.addProperty("nombreCompleto", cert.getIssuedTo());
                    firmaObj.addProperty("entidadCertificadora", cert.getIssuedBy());
                    firmaObj.addProperty("fechaEmision", formatCalendar(cert.getValidFrom()));
                    firmaObj.addProperty("fechaExpiracion", formatCalendar(cert.getValidTo()));
                    firmaObj.addProperty("fechaFirmado", formatCalendar(cert.getSignGenerated()));
                    firmaObj.addProperty("revocado", cert.getRevocated() != null);
                    firmaObj.addProperty("fechaRevocacion", cert.getRevocated() != null ? formatCalendar(cert.getRevocated()) : (String) null);
                    firmaObj.addProperty("certificadoVigente", cert.getCertificateValidated());
                    firmaObj.addProperty("integridadFirma", cert.getSignVerify());
                    firmaObj.addProperty("razon", cert.getDocReason());
                    firmaObj.addProperty("localizacion", cert.getDocLocation());
                    firmaObj.addProperty("usosLlave", cert.getKeyUsages());

                    // Sellado de tiempo
                    JsonObject tsObj = new JsonObject();
                    tsObj.addProperty("fecha", cert.getDocTimeStamp() != null ? formatDate(cert.getDocTimeStamp()) : (String) null);
                    tsObj.addProperty("emitidoPor", cert.getDocTimeStampIssuedBy());
                    tsObj.addProperty("valido", cert.getDocValidTimeStamp() != null && cert.getDocValidTimeStamp());
                    tsObj.addProperty("autoridad", cert.getCnTimeStamp());
                    firmaObj.add("selladoTiempo", tsObj);

                    // Datos del usuario
                    DatosUsuario du = cert.getDatosUsuario();
                    if (du != null) {
                        JsonObject duObj = new JsonObject();
                        duObj.addProperty("cedula", du.getCedula());
                        duObj.addProperty("nombreCompleto", (du.getNombre() != null ? du.getNombre() : "") + " " + (du.getApellido() != null ? du.getApellido() : ""));
                        duObj.addProperty("tipoCertificado", du.getTipoCertificado());
                        duObj.addProperty("certificadoDigitalValido", du.isCertificadoDigitalValido());
                        firmaObj.add("datosUsuario", duObj);
                    }

                    firmasArray.add(firmaObj);
                }
            }
            response.add("firmas", firmasArray);

            LOGGER.log(Level.INFO, "Verificación completada. Firmas: {0}", certificados != null ? certificados.size() : 0);
            return new Gson().toJson(response);

        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Error de formato: {0}", e.getMessage());
            return crearRespuestaError("Error de formato: El documento no está correctamente codificado en Base64");
        } catch (ec.gob.firmadigital.libreria.exceptions.InvalidFormatException e) {
            LOGGER.log(Level.SEVERE, "Formato de documento inválido: {0}", e.getMessage());
            return crearRespuestaError("El documento no es un PDF válido o no contiene firmas reconocibles");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al verificar documento: {0}", e);
            return crearRespuestaError("Error al verificar documento: " + e.getMessage());
        }
    }

    private String formatCalendar(Calendar cal) {
        if (cal == null) return null;
        return SDF.format(cal.getTime());
    }

    private String formatDate(Date date) {
        if (date == null) return null;
        return SDF.format(date);
    }

    private String crearRespuestaError(String mensaje) {
        JsonObject error = new JsonObject();
        error.addProperty("resultado", "ERROR");
        error.addProperty("mensaje", mensaje);
        error.addProperty("firmaValida", false);
        return new Gson().toJson(error);
    }
}
