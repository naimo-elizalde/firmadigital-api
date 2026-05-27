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
package ec.gob.firmadigital.api.utils;

import java.util.Base64;

/**
 * Utilidad compartida para decodificación Base64.
 * Intenta decodificación estándar primero; si falla usa MIME decoder
 * (más permisivo con saltos de línea).
 */
public class Base64Utils {

    private Base64Utils() {
    }

    public static byte[] decodificar(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            throw new IllegalArgumentException("Cadena Base64 vacía");
        }
        String cleaned = base64String.trim().replaceAll("\\s+", "");
        try {
            return Base64.getDecoder().decode(cleaned);
        } catch (IllegalArgumentException e1) {
            try {
                return Base64.getMimeDecoder().decode(cleaned);
            } catch (IllegalArgumentException e2) {
                throw new IllegalArgumentException(
                        "El contenido Base64 no es válido. Verifique que el contenido esté correctamente codificado.");
            }
        }
    }
}
