/*
 * Copyright (C) 2021
 * Authors: Ricardo Arguello
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
package ec.gob.firmadigital.libreria.sign.pdf.appearance;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.signatures.PdfSignatureAppearance;

import ec.gob.firmadigital.libreria.utils.QRCode;
import static ec.gob.firmadigital.libreria.utils.Utils.loadFont;
import java.util.logging.Level;

public class QrAppereance implements CustomAppearance {

    private final String nombreFirmante;
    private final String reason;
    private final String location;
    private final String signTime;
    private final String infoQR;

    private static final Logger LOGGER = Logger.getLogger(QrAppereance.class.getName());

    public QrAppereance(String nombreFirmante, String reason, String location, String signTime, String infoQR) {
        this.nombreFirmante = nombreFirmante;
        this.reason = reason;
        this.location = location;

        if (signTime == null || signTime.trim().isEmpty()) {
            this.signTime = ZonedDateTime.now(java.time.ZoneOffset.UTC)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        } else {
            this.signTime = signTime;
        }

        this.infoQR = infoQR;
    }

    @Override
    public void createCustomAppearance(PdfSignatureAppearance signatureAppearance, int pageNumber,
            PdfDocument pdfDocument, Rectangle signaturePositionOnPage) throws IOException {

        signatureAppearance.setPageRect(signaturePositionOnPage);
        signatureAppearance.setPageNumber(pageNumber);

        PdfFormXObject layer2 = signatureAppearance.getLayer2();
        PdfCanvas canvas = new PdfCanvas(layer2, pdfDocument);

        PdfFont fontCourier = loadFont("fonts/courier.ttf");
        PdfFont fontCourierBold = loadFont("fonts/courier-bold.ttf");

        // QR - Generar contenido del código QR
        StringBuilder sb = new StringBuilder();
        sb.append("FIRMADO POR: ").append(nombreFirmante.trim()).append("\n");
        sb.append("RAZON: ").append(reason != null && !reason.isEmpty() ? reason : "Firma digital").append("\n");
        if (location != null && !location.isEmpty()) {
            sb.append("LOCALIZACION: ").append(location).append("\n");
        }
        sb.append("FECHA: ").append(signTime).append("\n");
        sb.append("VALIDAR CON: ").append("https://corporacionelizalde.com/").append("\n");
        sb.append(infoQR);
        String text = sb.toString();

        // Cargar isologo para integrar pixelado en el QR
        byte[] isologoBytes = null;
        try (InputStream isologoStream = getClass().getClassLoader().getResourceAsStream("images/isologo.png")) {
            if (isologoStream != null) {
                isologoBytes = isologoStream.readAllBytes();
            } else {
                LOGGER.log(Level.WARNING, "No se encontró isologo.png en resources/images/");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al cargar isologo: {0}", e.getMessage());
        }

        // Generar QR con isologo nítido en el centro
        byte[] byteQR = null;
        try {
            byteQR = QRCode.generateQR(text, 300, 300, isologoBytes);
            LOGGER.log(Level.INFO, "QR generado: {0} bytes, isologo: {1}",
                    new Object[]{byteQR != null ? byteQR.length : 0, isologoBytes != null ? "si" : "no"});
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al generar QR con isologo", e);
            // Fallback: generar QR sin isologo
            try {
                byteQR = QRCode.generateQR(text, 300, 300);
                LOGGER.log(Level.INFO, "QR generado sin isologo (fallback)");
            } catch (Exception e2) {
                LOGGER.log(Level.SEVERE, "Error al generar QR fallback", e2);
            }
        }

        float totalWidth = signaturePositionOnPage.getWidth();
        float totalHeight = signaturePositionOnPage.getHeight();

        // Layout: [QR con isologo] [Texto firmante]
        float qrSide = totalHeight;
        float qrStartX = 0f;
        float textStartX = qrSide + 2f;

        // === 1. QR con isologo ===
        if (byteQR != null) {
            Rectangle qrRect = new Rectangle(qrStartX, 0, qrSide, qrSide);
            Image qrImage = new Image(ImageDataFactory.create(byteQR));
            qrImage.scaleToFit(qrSide, qrSide);
            qrImage.setMargins(0, 0, 0, 0);
            try (Canvas qrCanvas = new Canvas(canvas, qrRect)) {
                qrCanvas.add(qrImage);
            }
        }

        // === 4. Texto derecho: firmante ===
        float textWidth = totalWidth - textStartX;
        Rectangle signatureRect = new Rectangle(textStartX, 0, textWidth, totalHeight);

        Div textDiv = new Div();
        textDiv.setHeight(signatureRect.getHeight());
        textDiv.setWidth(signatureRect.getWidth());
        textDiv.setVerticalAlignment(VerticalAlignment.MIDDLE);
        textDiv.setHorizontalAlignment(HorizontalAlignment.LEFT);
        textDiv.setPaddingLeft(2f);

        // "Firmado electrónicamente por:"
        Text firmado = new Text("Firmado electrónicamente por:");
        Paragraph pFirmado = new Paragraph().add(firmado).setFont(fontCourier).setMargin(0)
                .setMultipliedLeading(1.0f).setFontSize(3.25f);
        textDiv.add(pFirmado);

        // Nombre en bold — dividir en dos líneas por mitad de palabras
        String nombreTrimmed = nombreFirmante.trim();
        String[] palabras = nombreTrimmed.split("\\s+");
        String linea1, linea2;
        if (palabras.length >= 2) {
            int mitad = palabras.length / 2;
            StringBuilder sb1 = new StringBuilder();
            StringBuilder sb2 = new StringBuilder();
            for (int i = 0; i < palabras.length; i++) {
                if (i < mitad) {
                    if (sb1.length() > 0) sb1.append(" ");
                    sb1.append(palabras[i]);
                } else {
                    if (sb2.length() > 0) sb2.append(" ");
                    sb2.append(palabras[i]);
                }
            }
            linea1 = sb1.toString();
            linea2 = sb2.toString();
        } else {
            linea1 = nombreTrimmed;
            linea2 = null;
        }
        Paragraph pNombre = new Paragraph().add(new Text(linea1)).setFont(fontCourierBold).setMargin(0)
                .setMultipliedLeading(0.9f).setFontSize(6.25f);
        textDiv.add(pNombre);
        if (linea2 != null) {
            Paragraph pNombre2 = new Paragraph().add(new Text(linea2)).setFont(fontCourierBold).setMargin(0)
                    .setMultipliedLeading(0.9f).setFontSize(6.25f);
            textDiv.add(pNombre2);
        }

        // Fecha
        Text fecha = new Text("Fecha: " + signTime);
        Paragraph pFecha = new Paragraph().add(fecha).setFont(fontCourier).setMargin(0)
                .setMultipliedLeading(1.0f).setFontSize(3.25f).setPaddingTop(2f);
        textDiv.add(pFecha);

        // Validar con
        Text validar = new Text("Validar únicamente con Corporación Elizalde.");
        Paragraph pValidar = new Paragraph().add(validar).setFont(fontCourier).setMargin(0)
                .setMultipliedLeading(1.0f).setFontSize(3.25f).setPaddingTop(1f);
        textDiv.add(pValidar);

        try (Canvas textLayoutCanvas = new Canvas(canvas, signatureRect)) {
            textLayoutCanvas.add(textDiv);
        }
    }
}
