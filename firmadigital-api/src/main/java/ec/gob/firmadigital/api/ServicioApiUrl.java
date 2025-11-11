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
import java.util.logging.Logger;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Base64;
import java.util.logging.Level;

/**
 * Permite validar si una API URL es permitida.
 * Versión standalone sin dependencias de servicios externos.
 *
 * @author Ricardo Arguello
 */
@Path("/url")
public class ServicioApiUrl {

    private static final Logger LOGGER = Logger.getLogger(ServicioApiUrl.class.getName());

    @GET
    @Path("{base64}")
    @Produces(MediaType.APPLICATION_JSON)
    public String validarEndpoint(@PathParam("base64") String base64) {
        LOGGER.log(Level.INFO, "Validando URL base64={0}", base64);
        
        try {
            // Decodificar la URL de base64
            String url = new String(Base64.getDecoder().decode(base64));
            LOGGER.log(Level.INFO, "URL decodificada: {0}", url);
            
            // Validación básica de URL
            boolean valida = validarUrl(url);
            
            JsonObject response = new JsonObject();
            response.addProperty("resultado", valida ? "OK" : "INVALID");
            response.addProperty("url", url);
            response.addProperty("valida", valida);
            response.addProperty("mensaje", valida ? "URL válida" : "URL no válida");
            
            return new Gson().toJson(response);
            
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Error al decodificar base64: {0}", e.getMessage());
            
            JsonObject error = new JsonObject();
            error.addProperty("resultado", "ERROR");
            error.addProperty("mensaje", "Base64 inválido");
            return new Gson().toJson(error);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al validar URL: {0}", e.getMessage());
            
            JsonObject error = new JsonObject();
            error.addProperty("resultado", "ERROR");
            error.addProperty("mensaje", "Error al validar URL: " + e.getMessage());
            return new Gson().toJson(error);
        }
    }
    
    /**
     * Valida si una URL tiene un formato válido y cumple con criterios básicos
     */
    private boolean validarUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        // Validar que comience con http:// o https://
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return false;
        }
        
        // Validar que tenga al menos un punto en el dominio
        if (!url.contains(".")) {
            return false;
        }
        
        // Validar longitud razonable
        if (url.length() > 2048) {
            return false;
        }
        
        return true;
    }
}
