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
package ec.gob.firmadigital.api.security;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Filtro para autenticación JWT en endpoints protegidos con @Secured.
 * 
 * Este filtro intercepta las peticiones a endpoints anotados con @Secured
 * y valida que contengan un token JWT válido. El token puede venir en:
 * 1. Header Authorization: "Bearer {token}"
 * 2. Parámetro jwt en el cuerpo de la petición (form-data)
 *
 * @author Christian Espinosa
 */
@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthenticationFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(JwtAuthenticationFilter.class.getName());
    
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Intentar obtener el token del header Authorization
        String authHeader = requestContext.getHeaderString("Authorization");
        String token = null;
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            LOGGER.log(Level.FINE, "Token JWT encontrado en header Authorization");
        } else {
            // Si no está en el header, intentar obtenerlo del cuerpo de la petición
            token = extractTokenFromBody(requestContext);
        }
        
        // Validar el token
        if (token == null || token.isEmpty()) {
            LOGGER.log(Level.WARNING, "Token JWT no proporcionado");
            abortRequest(requestContext, "Token JWT no proporcionado", Response.Status.UNAUTHORIZED);
            return;
        }
        
        // Validar el token usando JwtUtil
        if (!JwtUtil.validateToken(token)) {
            LOGGER.log(Level.WARNING, "Token JWT inválido o expirado");
            abortRequest(requestContext, "Token JWT inválido o expirado", Response.Status.UNAUTHORIZED);
            return;
        }
        
        // Token válido - continuar con la petición
        String subject = JwtUtil.getSubjectFromToken(token);
        LOGGER.log(Level.INFO, "Token JWT validado correctamente para: {0}", subject);
        
        // Opcionalmente, agregar información del usuario al contexto
        requestContext.setProperty("jwt.subject", subject);
    }
    
    /**
     * Intenta extraer el token JWT del cuerpo de la petición (parámetro jwt)
     */
    private String extractTokenFromBody(ContainerRequestContext requestContext) {
        try {
            // Solo procesar si es form-data
            MediaType mediaType = requestContext.getMediaType();
            if (mediaType == null || !mediaType.isCompatible(MediaType.APPLICATION_FORM_URLENCODED_TYPE)) {
                return null;
            }
            
            // Leer el cuerpo de la petición
            InputStream originalStream = requestContext.getEntityStream();
            String body = readInputStream(originalStream);
            
            // Restaurar el stream para que el endpoint pueda leerlo
            requestContext.setEntityStream(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
            
            // Parsear el cuerpo para extraer el parámetro jwt
            if (body.contains("jwt=")) {
                String[] params = body.split("&");
                for (String param : params) {
                    if (param.startsWith("jwt=")) {
                        String token = param.substring(4);
                        // Decodificar URL encoding si es necesario
                        token = java.net.URLDecoder.decode(token, StandardCharsets.UTF_8);
                        LOGGER.log(Level.FINE, "Token JWT encontrado en el cuerpo de la petición");
                        return token;
                    }
                }
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al extraer token del cuerpo: {0}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Lee un InputStream y lo convierte a String
     */
    private String readInputStream(InputStream stream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }
    
    /**
     * Aborta la petición con un error de autenticación
     */
    private void abortRequest(ContainerRequestContext requestContext, String message, Response.Status status) {
        JsonObject error = new JsonObject();
        error.addProperty("resultado", "ERROR");
        error.addProperty("mensaje", message);
        error.addProperty("codigo", status.getStatusCode());
        
        Response response = Response
                .status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new Gson().toJson(error))
                .build();
        
        requestContext.abortWith(response);
    }
}
