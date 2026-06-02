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

import com.itextpdf.kernel.pdf.PdfReader;

import java.io.ByteArrayInputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/appverificardocumento")
public class ServicioAppVerificarDocumento extends RequestSizeFilter {

    private static final Logger LOGGER = Logger.getLogger(ServicioAppVerificarDocumento.class.getName());
    private static final DateTimeFormatter ISO8601 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private static String fmtCal(Calendar cal) {
        return cal.toInstant().atZone(ZoneId.systemDefault()).format(ISO8601);
    }

    private static String fmtDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).format(ISO8601);
    }

    @POST
    @Secured
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String verificarDocumento(
            @FormParam("documento") String documentoBase64
    ) {
        LOGGER.log(Level.INFO, "Iniciando verificacion de documento firmado");

        try {
            if (documentoBase64 == null || documentoBase64.isEmpty()) {
                return crearRespuestaError("El documento es requerido");
            }

            byte[] docBytes = Base64Utils.decodificar(documentoBase64);

            PadesSigner padesSigner = new PadesSigner();
            List<SignInfo> signInfos = padesSigner.getSigners(docBytes);

            PdfReader pdfReader = new PdfReader(new ByteArrayInputStream(docBytes));
            Documento documento = Utils.pdfToDocumento(pdfReader, signInfos);

            if (documento.getCertificados() == null || documento.getCertificados().isEmpty()) {
                JsonObject response = new JsonObject();
                response.addProperty("resultado", "OK");
                response.addProperty("firmasValidas", false);
                response.addProperty("documentoIntegro", false);
                response.addProperty("numeroFirmas", 0);
                response.addProperty("mensaje", documento.getError() != null
                        ? documento.getError()
                        : "El documento no contiene firmas digitales");
                return new Gson().toJson(response);
            }

            JsonObject response = new JsonObject();
            response.addProperty("resultado", "OK");
            response.addProperty("firmasValidas", documento.getSignValidate());
            response.addProperty("documentoIntegro", documento.getDocValidate());
            response.addProperty("numeroFirmas", documento.getCertificados().size());
            if (documento.getError() != null) {
                response.addProperty("error", documento.getError());
            }

            JsonArray firmasArray = new JsonArray();
            int firmaIndex = 1;

            for (Certificado cert : documento.getCertificados()) {
                JsonObject f = new JsonObject();
                f.addProperty("numeroFirma", firmaIndex++);

                // Datos del firmante
                f.addProperty("nombreCompleto", cert.getIssuedTo());
                f.addProperty("entidadCertificadora", cert.getIssuedBy());

                // Fechas del certificado
                if (cert.getValidFrom() != null) {
                    f.addProperty("fechaEmision", fmtCal(cert.getValidFrom()));
                }
                if (cert.getValidTo() != null) {
                    f.addProperty("fechaExpiracion", fmtCal(cert.getValidTo()));
                }
                if (cert.getSignGenerated() != null) {
                    f.addProperty("fechaFirmado", fmtCal(cert.getSignGenerated()));
                }

                // Revocacion
                if (cert.getRevocated() != null) {
                    f.addProperty("fechaRevocacion", fmtCal(cert.getRevocated()));
                    f.addProperty("revocado", true);
                } else {
                    f.addProperty("revocado", false);
                }

                // Validaciones
                f.addProperty("certificadoVigente", cert.getCertificateValidated());
                f.addProperty("integridadFirma", cert.getSignVerify());

                // Metadatos de firma
                f.addProperty("razon", cert.getDocReason());
                f.addProperty("localizacion", cert.getDocLocation());
                f.addProperty("usosLlave", cert.getKeyUsages());

                // Sellado de Tiempo (TSA)
                f.addProperty("tieneSelladoTiempo", cert.getDocValidTimeStamp());
                if (cert.getDocTimeStamp() != null) {
                    JsonObject tsa = new JsonObject();
                    tsa.addProperty("fecha", fmtDate(cert.getDocTimeStamp()));
                    tsa.addProperty("emitidoPor", cert.getDocTimeStampIssuedBy());
                    tsa.addProperty("valido", cert.getDocValidTimeStamp());
                    if (cert.getCnTimeStamp() != null) {
                        tsa.addProperty("autoridad", cert.getCnTimeStamp());
                    }
                    f.add("selladoTiempo", tsa);
                }

                // Datos del usuario/firmante
                DatosUsuario datos = cert.getDatosUsuario();
                if (datos != null) {
                    JsonObject d = new JsonObject();
                    d.addProperty("cedula", datos.getCedula());
                    d.addProperty("nombreCompleto", datos.getNombre());
                    d.addProperty("tipoCertificado", datos.getTipoCertificado());
                    d.addProperty("certificadoDigitalValido", datos.isCertificadoDigitalValido());
                    f.add("datosUsuario", d);
                }

                firmasArray.add(f);
            }

            response.add("firmas", firmasArray);
            return new Gson().toJson(response);

        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Error de formato: {0}", e.getMessage());
            return crearRespuestaError("El documento no esta correctamente codificado en Base64");
        } catch (ec.gob.firmadigital.libreria.exceptions.InvalidFormatException e) {
            LOGGER.log(Level.SEVERE, "Formato de documento invalido: {0}", e.getMessage());
            return crearRespuestaError("El documento no es un PDF valido o no contiene firmas reconocibles");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al verificar documento: {0}", e);
            return crearRespuestaError("Error interno al verificar documento. Intente nuevamente");
        }
    }

    private String crearRespuestaError(String mensaje) {
        JsonObject error = new JsonObject();
        error.addProperty("resultado", "ERROR");
        error.addProperty("mensaje", mensaje);
        error.addProperty("firmasValidas", false);
        return new Gson().toJson(error);
    }
}
