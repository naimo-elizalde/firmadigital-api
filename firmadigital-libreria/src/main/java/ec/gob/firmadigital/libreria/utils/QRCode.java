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

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileInputStream;
import java.util.EnumMap;

public class QRCode {

    public static byte[] generateQR(String text, int h, int w) throws Exception {
        return generateQR(text, h, w, null);
    }

    public static byte[] generateQR(String text, int h, int w, byte[] overlayImageBytes) throws Exception {
        java.util.Map<com.google.zxing.EncodeHintType, Object> hints = new EnumMap<>(
                com.google.zxing.EncodeHintType.class);
        hints.put(com.google.zxing.EncodeHintType.CHARACTER_SET, java.nio.charset.StandardCharsets.US_ASCII.name());
        hints.put(com.google.zxing.EncodeHintType.MARGIN, 0);
        hints.put(com.google.zxing.EncodeHintType.ERROR_CORRECTION,
                overlayImageBytes != null
                        ? com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
                        : com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L);

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(text, com.google.zxing.BarcodeFormat.QR_CODE, w, h, hints);

        if (overlayImageBytes == null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "png", bos);
            bos.close();
            return bos.toByteArray();
        }

        // Renderizar QR con módulos redondeados + isologo nítido en el centro
        int matrixW = bitMatrix.getWidth();
        int matrixH = bitMatrix.getHeight();
        float moduleW = (float) w / matrixW;
        float moduleH = (float) h / matrixH;

        // Zona central para isologo (30% del QR)
        int overlaySize = (int) (w * 0.27);
        int overlayX = (w - overlaySize) / 2;
        int overlayY = (h - overlaySize) / 2;

        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);

        // Dibujar módulos QR redondeados (saltar zona central del isologo)
        Color qrBlack = new Color(30, 30, 30);
        float dotRatio = 0.85f;

        for (int row = 0; row < matrixH; row++) {
            for (int col = 0; col < matrixW; col++) {
                if (!bitMatrix.get(col, row)) continue;

                float px = col * moduleW;
                float py = row * moduleH;

                // Saltar módulos en la zona del isologo
                boolean inOverlayZone = (px + moduleW) > overlayX && px < (overlayX + overlaySize)
                        && (py + moduleH) > overlayY && py < (overlayY + overlaySize);
                if (inOverlayZone) continue;

                float dotW = moduleW * dotRatio;
                float dotH = moduleH * dotRatio;
                float dotX = px + (moduleW - dotW) / 2;
                float dotY = py + (moduleH - dotH) / 2;

                g.setColor(qrBlack);
                g.fillRoundRect((int) dotX, (int) dotY, Math.max(1, (int) dotW), Math.max(1, (int) dotH),
                        (int) (dotW * 0.4f), (int) (dotH * 0.4f));
            }
        }

        // Superponer isologo nítido en el centro
        BufferedImage overlay = ImageIO.read(new ByteArrayInputStream(overlayImageBytes));
        g.drawImage(overlay, overlayX, overlayY, overlaySize, overlaySize, null);

        g.dispose();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(result, "png", bos);
        bos.close();
        return bos.toByteArray();
    }

    public static String decoder(File file) throws Exception {
        FileInputStream inputStream = new FileInputStream(file);
        BufferedImage image = ImageIO.read(inputStream);
        LuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        QRCodeReader reader = new QRCodeReader();
        Result result = reader.decode(bitmap);
        return new String(result.getText());
    }
}
