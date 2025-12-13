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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import com.itextpdf.io.image.ImageDataFactory;
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
        
        // Si signTime es null o vacío, generar fecha actual
        if (signTime == null || signTime.trim().isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            this.signTime = sdf.format(new Date());
        } else {
            this.signTime = signTime;
        }
        
        this.location = location;
        this.signTime = signTime;
        this.infoQR = infoQR;
    }

    @Override
    public void createCustomAppearance(PdfSignatureAppearance signatureAppearance, int pageNumber,
            PdfDocument pdfDocument, Rectangle signaturePositionOnPage) throws IOException {

        signatureAppearance.setPageRect(signaturePositionOnPage);
        signatureAppearance.setPageNumber(pageNumber);

        String text = "FIRMADO POR: " + nombreFirmante.trim() + "\n";
        text = text + "RAZON: " + "Firmado digitalmente con Nexus Soluciones" + "\n";
        text = text + "LOCALIZACION: " + "ECUADOR" + "\n";
        text = text + "FECHA: " + signTime + "\n";
        // QR
        // Validar que signTime no sea null
        String fechaFirma = (signTime != null && !signTime.trim().isEmpty()) ? signTime : "";
        
        String text = "FIRMADO POR: " + nombreFirmante.trim() + "\n";
        text = text + "RAZON: " + "Firmado digitalmente con Nexus Soluciones" + "\n";
        text = text + "LOCALIZACION: " + "ECUADOR" + "\n";
        if (!fechaFirma.isEmpty()) {
            text = text + "FECHA: " + fechaFirma + "\n";
        }
        text = text + "VALIDAR CON: " + "https://www.solucionesnexus.com" + "\n";
        text = text + infoQR;

        try {
            byteQR = QRCode.generateQR(text, (int) signaturePositionOnPage.getHeight(),
                    (int) signaturePositionOnPage.getHeight());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al generar QR: {0}", e);
        }

        // QR
        Rectangle dataRect = new Rectangle(0, 0, signaturePositionOnPage.getWidth() / 3,
                signaturePositionOnPage.getHeight());

        // Aumentar separación entre QR y texto (ajustar el valor de división para más o menos espacio)
        float separacion = signaturePositionOnPage.getWidth() / 2.8f;
        Rectangle signatureRect = new Rectangle(separacion, 0,
                signaturePositionOnPage.getWidth() - separacion, signaturePositionOnPage.getHeight());

        Div imageDiv = new Div();
        imageDiv.setHeight(dataRect.getHeight());
        imageDiv.setWidth(dataRect.getWidth());
        imageDiv.setVerticalAlignment(VerticalAlignment.MIDDLE);
        imageDiv.setHorizontalAlignment(HorizontalAlignment.CENTER);

        Image image = new Image(ImageDataFactory.create(byteQR));
        image.setAutoScale(true);
        imageDiv.add(image);

        try (Canvas imageLayoutCanvas = new Canvas(canvas, dataRect)) {
            imageLayoutCanvas.add(imageDiv);
        }

        Div textDiv = new Div();
        textDiv.setHeight(signatureRect.getHeight());
        textDiv.setWidth(signatureRect.getWidth());
        textDiv.setVerticalAlignment(VerticalAlignment.MIDDLE);
        textDiv.setHorizontalAlignment(HorizontalAlignment.LEFT);

        Text texto = new Text("Firmado electrónicamente por:\n");
        Paragraph paragraph = new Paragraph().add(texto).setFont(fontCourier).setMargin(0).setMultipliedLeading(0.9f)
                .setFontSize(3.25f);
        textDiv.add(paragraph);

        Text contenido = new Text(nombreFirmante.trim());
        paragraph = new Paragraph().add(contenido).setFont(fontCourierBold).setMargin(0).setMultipliedLeading(0.9f)
                .setFontSize(6.25f);
        textDiv.add(paragraph);

        Text info = new Text("\nValidar únicamente con Nexus Soluciones\n");
        paragraph = new Paragraph().add(info).setFont(fontCourier).setMargin(0).setMultipliedLeading(0.9f)
                .setFontSize(3.25f);
        textDiv.add(paragraph);

        try (Canvas textLayoutCanvas = new Canvas(canvas, signatureRect)) {
            textLayoutCanvas.add(textDiv);
        }
    }
}
