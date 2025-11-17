/*
 * Firma Digital: API
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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Este servicio permite obtener la fecha y hora del servidor en formato
 * ISO-8601.
 * Versión standalone sin dependencias de servicios externos.
 *
 * @author Ricardo Arguello
 */
@Path("/fecha-hora")
public class ServicioFechaHora {

    /**
     * Retorna la fecha y hora del servidor, en formato ISO-8601.
     * Por ejemplo: "2025-11-11T17:54:43.562-05:00"
     *
     * @param base64 Token de validación (opcional)
     * @return Fecha y hora en formato ISO-8601
     */
    @POST
    @Secured
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public String getFechaHora(@FormParam("base64") String base64) {
        // Retorna directamente la fecha y hora del servidor
        return ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * Endpoint GET para obtener la fecha y hora en formato JSON
     *
     * @return JSON con fecha, hora y zona horaria
     */
    @GET
    @Secured
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFechaHoraJson() {
        ZonedDateTime now = ZonedDateTime.now();
        
        JsonObject response = new JsonObject();
        response.addProperty("fechaHora", now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        response.addProperty("timestamp", now.toEpochSecond());
        response.addProperty("zonaHoraria", now.getZone().getId());
        response.addProperty("resultado", "OK");
        
        return Response.ok(new Gson().toJson(response)).build();
    }
}
