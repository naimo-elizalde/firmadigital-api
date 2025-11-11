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
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Properties;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Permite validar la versión permitido.
 * Versión standalone sin dependencias de servicios externos.
 *
 * @author Christian Espinosa, Misael Fernández
 */
@Path("/version")
public class ServicioVersion {

    private static final String VERSION_PROPERTY = "version";
    private static final String CONFIG_FILE = "config.api.properties";

    /**
     * Obtiene la versión de la API desde el archivo de configuración
     */
    private String getVersion() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                return "4.1.0"; // Versión por defecto
            }
            Properties prop = new Properties();
            prop.load(input);
            return prop.getProperty(VERSION_PROPERTY, "4.1.0");
        } catch (IOException e) {
            return "4.1.0";
        }
    }

    /**
     * Valida la versión del cliente comparándola con la versión del servidor
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public String validarEndpoint(@FormParam("base64") String base64) {
        try {
            String versionServidor = getVersion();
            String versionCliente = null;
            
            // Decodificar la versión del cliente si viene en base64
            if (base64 != null && !base64.isEmpty()) {
                try {
                    versionCliente = new String(Base64.getDecoder().decode(base64));
                } catch (IllegalArgumentException e) {
                    // Si no es base64 válido, usar como está
                    versionCliente = base64;
                }
            }
            
            // Crear respuesta JSON
            JsonObject response = new JsonObject();
            response.addProperty("resultado", "Version enabled");
            response.addProperty("versionServidor", versionServidor);
            
            if (versionCliente != null) {
                response.addProperty("versionCliente", versionCliente);
                response.addProperty("compatible", versionServidor.equals(versionCliente));
            }
            
            return new Gson().toJson(response);
            
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("resultado", "ERROR");
            error.addProperty("mensaje", "Error al validar versión: " + e.getMessage());
            return new Gson().toJson(error);
        }
    }

    /**
     * Endpoint GET para obtener la versión directamente
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVersionInfo() {
        JsonObject response = new JsonObject();
        response.addProperty("version", getVersion());
        response.addProperty("resultado", "OK");
        return Response.ok(new Gson().toJson(response)).build();
    }
}
