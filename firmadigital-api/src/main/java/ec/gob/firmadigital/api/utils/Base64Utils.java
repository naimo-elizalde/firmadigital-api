package ec.gob.firmadigital.api.utils;

import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Base64Utils {

    private static final Logger LOGGER = Logger.getLogger(Base64Utils.class.getName());

    public static byte[] decodificar(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            throw new IllegalArgumentException("Cadena Base64 vacia");
        }

        String cleaned = base64String.replaceAll("\\s+", "");

        try {
            return Base64.getDecoder().decode(cleaned);
        } catch (IllegalArgumentException e1) {
            LOGGER.log(Level.WARNING, "Fallo decodificacion estandar, intentando con MIME decoder: {0}", e1.getMessage());

            try {
                return Base64.getMimeDecoder().decode(cleaned);
            } catch (IllegalArgumentException e2) {
                LOGGER.log(Level.SEVERE, "Error al decodificar Base64 con ambos decoders: {0}", e2.getMessage());
                throw new IllegalArgumentException("El contenido Base64 no es valido. Verifique que el contenido este correctamente codificado.");
            }
        }
    }
}
