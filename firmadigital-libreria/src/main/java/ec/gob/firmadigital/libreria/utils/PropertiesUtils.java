/*
 * Copyright (C) 2020
 * Authors: Ricardo Arguello, Misael Fernandez
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilidad para menejar properties
 *
 * @author Misael Fernandez
 */
public class PropertiesUtils {

    private static final Logger LOGGER = Logger.getLogger(PropertiesUtils.class.getName());
    private static final String MESSAGES = "messages.rubrica.properties";
    private static final String CONFIG = "config.rubrica.properties";
    private static volatile Properties messages;
    private static volatile Properties config;

    public static Properties getMessages() {
        if (messages == null) {
            synchronized (PropertiesUtils.class) {
                if (messages == null) {
                    Properties props = new Properties();
                    try (InputStream is = PropertiesUtils.class.getClassLoader().getResourceAsStream(MESSAGES)) {
                        if (is != null) {
                            props.load(is);
                        }
                    } catch (IOException ex) {
                        LOGGER.log(Level.SEVERE, "Error al cargar messages.rubrica.properties", ex);
                    }
                    messages = props;
                }
            }
        }
        return messages;
    }

    public static Properties getConfig() {
        if (config == null) {
            synchronized (PropertiesUtils.class) {
                if (config == null) {
                    Properties props = new Properties();
                    try (InputStream is = PropertiesUtils.class.getClassLoader().getResourceAsStream(CONFIG)) {
                        if (is != null) {
                            props.load(is);
                        }
                    } catch (IOException ex) {
                        LOGGER.log(Level.SEVERE, "Error al cargar config.rubrica.properties", ex);
                    }
                    config = props;
                }
            }
        }
        return config;
    }

    public static String versionBase64() {
        String jsonVersion = Json.generarJsonVersion(OsUtils.getOs(), "LIBRERIA", getConfig().getProperty("version"));
        String base64 = java.util.Base64.getEncoder().encodeToString(jsonVersion.getBytes());
        return base64;
    }
}
