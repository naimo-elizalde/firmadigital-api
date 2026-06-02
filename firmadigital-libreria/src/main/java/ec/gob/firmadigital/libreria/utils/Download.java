/*
 * Copyright (C) 2020 
 * Authors: Ricardo Arguello, Misael Fernández
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 * @author Misael Fernández
 */
public class Download {

    private static final int TIME_OUT = 5000; //set timeout to 5 seconds
    private static final int BUFFER_SIZE = 8192;
    private static final Logger LOGGER = Logger.getLogger(Download.class.getName());

    public static void downloadCFG() {
        String path = System.getProperty("user.home") + "/cfg";
        File fileCfg = new File(path);

        if (!fileCfg.exists()) {
            try {
                // Stream ZIP directamente a disco sin byte[] intermediario
                URL cfgDownloadUrl = new URL(PropertiesUtils.getConfig().getProperty("cfg"));
                HttpURLConnection cfgCon = (HttpURLConnection) cfgDownloadUrl.openConnection();
                try {
                    int cfgCode = cfgCon.getResponseCode();
                    if (cfgCode >= 300 && cfgCode < 400) {
                        HttpURLConnection redirectCon = (HttpURLConnection) new URL(cfgCon.getHeaderField("Location")).openConnection();
                        cfgCon.disconnect();
                        cfgCon = redirectCon;
                        cfgCode = cfgCon.getResponseCode();
                    }
                    if (cfgCode >= 400) {
                        throw new IOException("Failed: HTTP error code: " + cfgCode);
                    }
                    byte[] dlBuf = new byte[BUFFER_SIZE];
                    int dlLen;
                    try (InputStream cfgStream = cfgCon.getInputStream();
                         FileOutputStream fileOuputStream = new FileOutputStream(path + ".zip")) {
                        while ((dlLen = cfgStream.read(dlBuf)) != -1) {
                            fileOuputStream.write(dlBuf, 0, dlLen);
                        }
                    }
                } finally {
                    cfgCon.disconnect();
                }

                File filePath = new File(path);
                byte[] buffer = new byte[1024];
                ZipInputStream zis = new ZipInputStream(new FileInputStream(path + ".zip"));
                ZipEntry zipEntry = zis.getNextEntry();
                while (zipEntry != null) {
                    File newFile = newFile(filePath, zipEntry);
                    if (zipEntry.isDirectory()) {
                        if (!newFile.isDirectory() && !newFile.mkdirs()) {
                            LOGGER.log(Level.WARNING, "Failed to create directory {0}", newFile);
                            throw new IOException("Failed to create directory " + newFile);
                        }
                    } else {
                        // fix for Windows-created archives
                        File parent = newFile.getParentFile();
                        if (!parent.isDirectory() && !parent.mkdirs()) {
                            LOGGER.log(Level.WARNING, "Failed to create directory {0}", parent);
                            throw new IOException("Failed to create directory " + parent);
                        }
                        // write file content
                        FileOutputStream fos = new FileOutputStream(newFile);
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        LOGGER.log(Level.INFO, "Unzipping to {0}", newFile.getAbsolutePath());
                        fos.close();
                    }
                    zipEntry = zis.getNextEntry();
                }
                zis.closeEntry();
                zis.close();
                LOGGER.info("Directorio cfg integrado con éxito!");

                File fileZip = new File(path + ".zip");
                fileZip.delete();
                LOGGER.info("Fichero zip borrado");
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        return destFile;
    }

    private static byte[] download(String strUrl, boolean controlTiempo) throws IOException {
        URL url = new URL(strUrl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        if (controlTiempo) {
            con.setConnectTimeout(TIME_OUT);
        }

        try {
            int responseCode = con.getResponseCode();
            if (responseCode >= 300 && responseCode < 400) {
                HttpURLConnection redirectCon = (HttpURLConnection) new URL(con.getHeaderField("Location")).openConnection();
                con.disconnect();
                con = redirectCon;
                if (controlTiempo) {
                    con.setConnectTimeout(TIME_OUT);
                }
                responseCode = con.getResponseCode();
            }
            if (responseCode >= 400) {
                throw new RuntimeException("Failed : HTTP error code : " + responseCode);
            }

            byte[] buffer = new byte[BUFFER_SIZE];
            int count;
            long size = con.getContentLength();
            LOGGER.log(Level.INFO, "size={0}", size);

            try (InputStream in = con.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                while ((count = in.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
                return out.toByteArray();
            }
        } finally {
            con.disconnect();
        }
    }
}
