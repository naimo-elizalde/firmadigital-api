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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilidad para manejo de tokens JWT.
 * Permite validar tokens JWT usando una clave secreta configurada.
 *
 * @author Christian Espinosa
 */
public class JwtUtil {

    private static final Logger LOGGER = Logger.getLogger(JwtUtil.class.getName());
    private static final String JWT_SECRET_ENV_VAR = "JWT_SECRET";
    
    private static SecretKey secretKey;
    
    static {
        initializeSecretKey();
    }
    
    /**
     * Inicializa la clave secreta desde la variable de entorno JWT_SECRET
     */
    private static void initializeSecretKey() {
        try {
            String secret = loadSecretFromEnvironment();
            if (secret == null || secret.isEmpty()) {
                throw new IllegalStateException(
                    "JWT_SECRET no está configurado. Por favor, configure la variable de entorno JWT_SECRET"
                );
            }
            // Usar el secret directamente como UTF-8 (compatible con NestJS/Node.js)
            byte[] keyBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            secretKey = Keys.hmacShaKeyFor(keyBytes);
            LOGGER.log(Level.INFO, "JWT secret key inicializada correctamente desde variable de entorno (UTF-8)");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "CRÍTICO: No se pudo inicializar JWT secret key: {0}", e.getMessage());
            throw new RuntimeException(
                "No se puede iniciar la aplicación sin JWT_SECRET configurado. " +
                "Configure la variable de entorno JWT_SECRET con una clave de al menos 256 bits.", e
            );
        }
    }
    
    /**
     * Carga el secreto JWT desde la variable de entorno o system property
     */
    private static String loadSecretFromEnvironment() {
        // Intentar primero desde variable de entorno
        String secret = System.getenv(JWT_SECRET_ENV_VAR);
        
        // Si no está en variable de entorno, intentar system property
        if (secret == null || secret.trim().isEmpty()) {
            secret = System.getProperty(JWT_SECRET_ENV_VAR);
            if (secret != null) {
                LOGGER.log(Level.INFO, "JWT_SECRET cargado desde System Property");
            }
        } else {
            LOGGER.log(Level.INFO, "JWT_SECRET cargado desde variable de entorno");
        }
        
        if (secret == null || secret.trim().isEmpty()) {
            LOGGER.log(Level.SEVERE, 
                "Variable de entorno o System Property {0} no configurada. La aplicación no puede procesar tokens JWT.",
                JWT_SECRET_ENV_VAR
            );
            return null;
        }
        
        // Log de los primeros caracteres para debug (sin exponer todo el secreto)
        LOGGER.log(Level.INFO, "JWT_SECRET encontrado (primeros 10 chars): {0}...", 
                  secret.substring(0, Math.min(10, secret.length())));
        LOGGER.log(Level.INFO, "JWT_SECRET longitud: {0} caracteres", secret.length());
        
        // Validar longitud mínima (256 bits = 32 bytes = 32 caracteres para UTF-8)
        if (secret.length() < 32) {
            LOGGER.log(Level.SEVERE, 
                "La clave JWT debe tener al menos 32 caracteres (256 bits). Longitud actual: {0} caracteres",
                secret.length()
            );
            return null;
        }
        
        return secret;
    }
    
    /**
     * Valida un token JWT
     * 
     * @param token Token JWT a validar
     * @return true si el token es válido, false en caso contrario
     */
    public static boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            LOGGER.log(Level.WARNING, "Token vacío o nulo");
            return false;
        }
        
        LOGGER.log(Level.INFO, "=== JwtUtil.validateToken - Iniciando validación ===");
        LOGGER.log(Level.INFO, "Token (primeros 30 chars): {0}...", token.substring(0, Math.min(30, token.length())));
        
        try {
            // Limpiar el token (remover "Bearer " si existe)
            token = cleanToken(token);
            
            LOGGER.log(Level.INFO, "Token después de limpiar: {0}...", token.substring(0, Math.min(30, token.length())));
            LOGGER.log(Level.INFO, "SecretKey presente: {0}", secretKey != null ? "SÍ" : "NO");
            
            // Parsear y validar el token
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            LOGGER.log(Level.INFO, "Claims extraídos exitosamente");
            LOGGER.log(Level.INFO, "Subject: {0}", claims.getSubject());
            LOGGER.log(Level.INFO, "ID: {0}", claims.getId());
            LOGGER.log(Level.INFO, "Expiration: {0}", claims.getExpiration());
            
            // Verificar expiración
            Date expiration = claims.getExpiration();
            if (expiration != null && expiration.before(new Date())) {
                LOGGER.log(Level.WARNING, "✗ Token expirado. Fecha exp: {0}, Fecha actual: {1}", 
                          new Object[]{expiration, new Date()});
                return false;
            }
            
            LOGGER.log(Level.INFO, "✓ Token validado correctamente para subject: {0}", claims.getSubject());
            return true;
            
        } catch (ExpiredJwtException e) {
            LOGGER.log(Level.WARNING, "✗ Token expirado: {0}", e.getMessage());
            LOGGER.log(Level.WARNING, "Claims: {0}", e.getClaims());
            return false;
        } catch (JwtException e) {
            LOGGER.log(Level.WARNING, "✗ Token inválido (JwtException): {0}", e.getMessage());
            LOGGER.log(Level.WARNING, "Causa: {0}", e.getCause());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "✗ Error al validar token: {0}", e.getMessage());
            LOGGER.log(Level.SEVERE, "Clase de excepción: {0}", e.getClass().getName());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Extrae el subject (usuario) de un token JWT
     * 
     * @param token Token JWT
     * @return Subject del token o null si no es válido
     */
    public static String getSubjectFromToken(String token) {
        try {
            token = cleanToken(token);
            
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.getSubject();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al extraer subject del token: {0}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Genera un token JWT (útil para testing)
     * 
     * @param subject Subject del token (usuario/identificador)
     * @param expirationMinutes Minutos hasta la expiración
     * @return Token JWT generado
     */
    public static String generateToken(String subject, long expirationMinutes) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + (expirationMinutes * 60 * 1000));
        
        return Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }
    
    /**
     * Limpia el token removiendo prefijos como "Bearer "
     */
    private static String cleanToken(String token) {
        if (token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }
    
    /**
     * Obtiene la fecha de expiración de un token
     * 
     * @param token Token JWT
     * @return Fecha de expiración o null si no es válido
     */
    public static Date getExpirationDate(String token) {
        try {
            token = cleanToken(token);
            
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.getExpiration();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al extraer fecha de expiración: {0}", e.getMessage());
            return null;
        }
    }
}
