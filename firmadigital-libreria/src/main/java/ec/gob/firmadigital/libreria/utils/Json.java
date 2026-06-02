/*
 * Copyright (C) 2024
 * Authors: Misael Fernandez
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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

import ec.gob.firmadigital.libreria.certificate.to.Certificado;
import ec.gob.firmadigital.libreria.certificate.to.DatosUsuario;
import ec.gob.firmadigital.libreria.certificate.to.Documento;

/**
 *
 * @author Misael Fernandez
 */
public class Json {

    private static final DateTimeFormatter ISO8601_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private static String formatCalendar(Calendar cal) {
        return cal.toInstant().atZone(ZoneId.systemDefault()).format(ISO8601_FORMATTER);
    }

    private static String formatDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).format(ISO8601_FORMATTER);
    }

    public static String generarJsonVersion(String sistemaOperativo, String aplicacion, String versionApp) {
        if (sistemaOperativo != null && versionApp != null) {
            JsonObject gsonObject = new JsonObject();
            gsonObject.addProperty("sistemaOperativo", sistemaOperativo);
            gsonObject.addProperty("aplicacion", aplicacion);
            gsonObject.addProperty("versionApp", versionApp);
            return gsonObject.toString();
        } else {
            return null;
        }
    }

    public static String generarJsonDocumento(Documento documento) {
        return generarJsonDocumentoFirmado(null, documento);
    }

    public static String generarJsonDocumentoFirmado(byte[] byteDocumentoSigned, Documento documento) {
        JsonArray gsonArray = new JsonArray();
        JsonObject jsonObjectDocumento = new JsonObject();
        jsonObjectDocumento.addProperty("signValidate", documento.getSignValidate());
        jsonObjectDocumento.addProperty("docValidate", documento.getDocValidate());
        if (byteDocumentoSigned != null) {
            jsonObjectDocumento.addProperty("docSigned", java.util.Base64.getEncoder().encodeToString(byteDocumentoSigned));
        }
        jsonObjectDocumento.addProperty("error", documento.getError());

        JsonArray jsonDocumentoArray = new JsonArray();
        for (Certificado certificado : documento.getCertificados()) {
            JsonObject jsonObjectCertificado = new JsonObject();
            jsonObjectCertificado.addProperty("issuedTo", certificado.getIssuedTo());
            jsonObjectCertificado.addProperty("issuedBy", certificado.getIssuedBy());
            jsonObjectCertificado.addProperty("validFrom", formatCalendar(certificado.getValidFrom()));
            jsonObjectCertificado.addProperty("validTo", formatCalendar(certificado.getValidTo()));
            jsonObjectCertificado.addProperty("generated", formatCalendar(certificado.getSignGenerated()));
            if (certificado.getRevocated() != null) {
                jsonObjectCertificado.addProperty("revocated", formatCalendar(certificado.getRevocated()));
            }
            jsonObjectCertificado.addProperty("certificateValidated", certificado.getCertificateValidated());
            jsonObjectCertificado.addProperty("keyUsages", certificado.getKeyUsages());
            if (certificado.getDocTimeStamp() != null) {
                jsonObjectCertificado.addProperty("docTimeStamp", formatDate(certificado.getDocTimeStamp()));
                jsonObjectCertificado.addProperty("docTimeStampIssuedBy", certificado.getDocTimeStampIssuedBy());
                jsonObjectCertificado.addProperty("docValidTimeStamp", certificado.getDocValidTimeStamp());
            }
            jsonObjectCertificado.addProperty("signVerify", certificado.getSignVerify());
            jsonObjectCertificado.addProperty("docReason", certificado.getDocReason());
            jsonObjectCertificado.addProperty("docLocation", certificado.getDocLocation());

            String json = generarJsonDatosUsuario(certificado.getDatosUsuario());
            JsonObject jsonObjectDatosUsuario = new Gson().fromJson(json, JsonObject.class);
            jsonObjectCertificado.add("datosUsuario", jsonObjectDatosUsuario);

            jsonDocumentoArray.add(jsonObjectCertificado);
        }
        jsonObjectDocumento.add("certificado", new JsonParser()
                .parse(new Gson().toJson(jsonDocumentoArray)).getAsJsonArray());
        gsonArray.add(jsonObjectDocumento);
        return gsonArray.toString();
    }

    public static String generarJsonDocumentoFirmadoTransversal(byte[] byteDocumentoSigned, Documento documento) {
        JsonArray gsonArray = new JsonArray();
        JsonObject jsonObjectDocumento = new JsonObject();
        jsonObjectDocumento.addProperty("validarFirma", documento.getSignValidate());
        jsonObjectDocumento.addProperty("validarDocumento", documento.getDocValidate());
        if (byteDocumentoSigned != null) {
            jsonObjectDocumento.addProperty("documentoFirmado", java.util.Base64.getEncoder().encodeToString(byteDocumentoSigned));
        }
        jsonObjectDocumento.addProperty("error", documento.getError());

        JsonArray jsonDocumentoArray = new JsonArray();
        for (Certificado certificado : documento.getCertificados()) {
            JsonObject jsonObjectCertificado = new JsonObject();
            jsonObjectCertificado.addProperty("cedula", certificado.getDatosUsuario().getCedula());
            jsonObjectCertificado.addProperty("nombresApeilldos", certificado.getIssuedTo());
            jsonObjectCertificado.addProperty("emitidoPor", certificado.getIssuedBy());
            jsonObjectCertificado.addProperty("validoDesde", formatCalendar(certificado.getValidFrom()));
            jsonObjectCertificado.addProperty("validoHasta", formatCalendar(certificado.getValidTo()));
            jsonObjectCertificado.addProperty("fechaRevocado", certificado.getRevocated() != null ? formatCalendar(certificado.getRevocated()) : null);
            jsonObjectCertificado.addProperty("certificadoDigitalValido", certificado.getDatosUsuario().isCertificadoDigitalValido());
            jsonObjectCertificado.addProperty("fechaDocumentoFirmado", formatCalendar(certificado.getSignGenerated()));
            jsonObjectCertificado.addProperty("razon", certificado.getDocReason());
            jsonObjectCertificado.addProperty("localizacion", certificado.getDocLocation());
            if (certificado.getDocTimeStamp() != null) {
                jsonObjectCertificado.addProperty("selladoTiempoFecha", formatDate(certificado.getDocTimeStamp()));
                jsonObjectCertificado.addProperty("selladoTiempoEmitidoPor", certificado.getDocTimeStampIssuedBy());
                jsonObjectCertificado.addProperty("selladoTiempoValido", certificado.getDocValidTimeStamp());
            }
            jsonDocumentoArray.add(jsonObjectCertificado);
        }
        jsonObjectDocumento.add("certificado", new JsonParser()
                .parse(new Gson().toJson(jsonDocumentoArray)).getAsJsonArray());
        gsonArray.add(jsonObjectDocumento);
        return gsonArray.toString();
    }

    public static String generarJsonCertificado(Certificado certificado) {
        JsonArray gsonArray = new JsonArray();
        JsonObject jsonObjectCertificado = new JsonObject();
        jsonObjectCertificado.addProperty("issuedTo", certificado.getIssuedTo());
        jsonObjectCertificado.addProperty("issuedBy", certificado.getIssuedBy());
        jsonObjectCertificado.addProperty("validFrom", formatCalendar(certificado.getValidFrom()));
        jsonObjectCertificado.addProperty("validTo", formatCalendar(certificado.getValidTo()));
        if (certificado.getSignGenerated() != null) {
            jsonObjectCertificado.addProperty("generated", formatCalendar(certificado.getSignGenerated()));
        }
        if (certificado.getRevocated() != null) {
            jsonObjectCertificado.addProperty("revocated", formatCalendar(certificado.getRevocated()));
        }
        jsonObjectCertificado.addProperty("validated", certificado.getCertificateValidated());
        jsonObjectCertificado.addProperty("keyUsages", certificado.getKeyUsages());
        if (certificado.getDocTimeStamp() != null) {
            jsonObjectCertificado.addProperty("docTimeStamp", formatDate(certificado.getDocTimeStamp()));
            jsonObjectCertificado.addProperty("docTimeStampIssuedBy", certificado.getDocTimeStampIssuedBy());
            jsonObjectCertificado.addProperty("docValidTimeStamp", certificado.getDocValidTimeStamp());
        }
        jsonObjectCertificado.addProperty("signVerify", certificado.getSignVerify());
        jsonObjectCertificado.addProperty("docReason", certificado.getDocReason());
        jsonObjectCertificado.addProperty("docLocation", certificado.getDocLocation());

        String json = null;
        if (certificado.getDatosUsuario() != null) {
            json = generarJsonDatosUsuario(certificado.getDatosUsuario());
        }
        JsonObject jsonObjectDatosUsuario = new Gson().fromJson(json, JsonObject.class);
        jsonObjectCertificado.add("datosUsuario", jsonObjectDatosUsuario);

        gsonArray.add(jsonObjectCertificado);
        return gsonArray.toString();
    }

    public static String generarJsonCertificadoTransversal(Certificado certificado) {
        JsonArray gsonArray = new JsonArray();
        JsonObject jsonObjectCertificado = null;
        if (certificado.getDatosUsuario() != null) {
            jsonObjectCertificado = new JsonObject();
            jsonObjectCertificado.addProperty("cedula", certificado.getDatosUsuario().getCedula());
            jsonObjectCertificado.addProperty("nombresApeilldos", certificado.getIssuedTo());
            jsonObjectCertificado.addProperty("emitidoPor", certificado.getIssuedBy());
            jsonObjectCertificado.addProperty("validoDesde", formatCalendar(certificado.getValidFrom()));
            jsonObjectCertificado.addProperty("validoHasta", formatCalendar(certificado.getValidTo()));
            jsonObjectCertificado.addProperty("fechaRevocado", certificado.getRevocated() != null ? formatCalendar(certificado.getRevocated()) : null);
        }
        gsonArray.add(jsonObjectCertificado);
        return gsonArray.toString();
    }

    public static String generarJsonDatosUsuario(DatosUsuario datosUsuario) {
        JsonObject jsonObjectDatosUsuario = new JsonObject();
        jsonObjectDatosUsuario.addProperty("cedula", datosUsuario.getCedula());
        jsonObjectDatosUsuario.addProperty("nombre", datosUsuario.getNombre());
        jsonObjectDatosUsuario.addProperty("apellido", datosUsuario.getApellido());
        jsonObjectDatosUsuario.addProperty("institucion", datosUsuario.getInstitucion());
        jsonObjectDatosUsuario.addProperty("cargo", datosUsuario.getCargo());
        jsonObjectDatosUsuario.addProperty("tipoCertificado", datosUsuario.getTipoCertificado());
        jsonObjectDatosUsuario.addProperty("certificadoDigitalValido", datosUsuario.isCertificadoDigitalValido());
        return jsonObjectDatosUsuario.toString();
    }

}
