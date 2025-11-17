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
import ec.gob.firmadigital.api.security.JwtUtil;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio para generar tokens JWT.
 * 
 * Este endpoint permite generar tokens JWT para autenticación.
 * En un entorno de producción, este servicio debería estar protegido
 * con credenciales de usuario/contraseña o integrado con un sistema
 * de autenticación existente.
 *
 * @author Christian Espinosa
 */
@Path("/auth")
public class ServicioAuth {

    private static final Logger LOGGER = Logger.getLogger(ServicioAuth.class.getName());
    
    /**
     * Genera un token JWT para el subject proporcionado.
     * 
     * NOTA: En producción, este endpoint debe validar credenciales
     * antes de generar el token.
     * 
     * @param subject Identificador del usuario/aplicación
     * @param expirationMinutes Minutos hasta la expiración (opcional, por defecto 60)
     * @return JSON con el token generado
     */
    @POST
    @Path("/token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public String generateToken(
            @FormParam("subject") String subject,
            @FormParam("expirationMinutes") String expirationMinutes
    ) {
        try {
            // Validar parámetros
            if (subject == null || subject.trim().isEmpty()) {
                return crearRespuestaError("El parámetro 'subject' es requerido");
            }
            
            // Parsear tiempo de expiración (por defecto 60 minutos)
            long expMinutes = 60;
            if (expirationMinutes != null && !expirationMinutes.isEmpty()) {
                try {
                    expMinutes = Long.parseLong(expirationMinutes);
                    if (expMinutes <= 0 || expMinutes > 1440) { // Máximo 24 horas
                        return crearRespuestaError("El tiempo de expiración debe estar entre 1 y 1440 minutos");
                    }
                } catch (NumberFormatException e) {
                    return crearRespuestaError("El parámetro 'expirationMinutes' debe ser un número válido");
                }
            }
            
            // Generar token
            String token = JwtUtil.generateToken(subject, expMinutes);
            
            LOGGER.log(Level.INFO, "Token JWT generado para subject: {0}, expiración: {1} minutos", 
                      new Object[]{subject, expMinutes});
            
            // Crear respuesta
            JsonObject response = new JsonObject();
            response.addProperty("resultado", "OK");
            response.addProperty("token", token);
            response.addProperty("subject", subject);
            response.addProperty("expirationMinutes", expMinutes);
            response.addProperty("tipo", "Bearer");
            
            return new Gson().toJson(response);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al generar token: {0}", e.getMessage());
            return crearRespuestaError("Error al generar token: " + e.getMessage());
        }
    }
    
    /**
     * Valida un token JWT y retorna su información.
     * 
     * @param token Token JWT a validar
     * @return JSON con información del token o error
     */
    @POST
    @Path("/validate")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public String validateToken(@FormParam("token") String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return crearRespuestaError("El parámetro 'token' es requerido");
            }
            
            // Validar token
            boolean isValid = JwtUtil.validateToken(token);
            
            JsonObject response = new JsonObject();
            response.addProperty("resultado", "OK");
            response.addProperty("valido", isValid);
            
            if (isValid) {
                String subject = JwtUtil.getSubjectFromToken(token);
                response.addProperty("subject", subject);
                
                java.util.Date expiration = JwtUtil.getExpirationDate(token);
                if (expiration != null) {
                    response.addProperty("expiracion", expiration.toString());
                }
            } else {
                response.addProperty("mensaje", "Token inválido o expirado");
            }
            
            return new Gson().toJson(response);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al validar token: {0}", e.getMessage());
            return crearRespuestaError("Error al validar token: " + e.getMessage());
        }
    }
    
    private String crearRespuestaError(String mensaje) {
        JsonObject error = new JsonObject();
        error.addProperty("resultado", "ERROR");
        error.addProperty("mensaje", mensaje);
        return new Gson().toJson(error);
    }
}
