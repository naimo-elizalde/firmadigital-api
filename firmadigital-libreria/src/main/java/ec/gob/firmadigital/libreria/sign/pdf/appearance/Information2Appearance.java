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

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.signatures.PdfSignatureAppearance;

import static ec.gob.firmadigital.libreria.utils.Utils.loadFont;

//Avanzado
public class Information2Appearance implements CustomAppearance {

    private final String nombreFirmante;
    private final String informacionCertificado;
    private final String reason;
    private final String location;
    private final String signTime;

    public Information2Appearance(String nombreFirmante, String informacionCertificado, String reason, String location,
            String signTime) {
        this.nombreFirmante = nombreFirmante;
        this.informacionCertificado = informacionCertificado;
        this.reason = reason;
        this.location = location;
        this.signTime = signTime;
    }

    @Override
    public void createCustomAppearance(PdfSignatureAppearance signatureAppearance, int pageNumber,
            PdfDocument pdfDocument, Rectangle signaturePositionOnPage) throws IOException {

        PdfFont fontHelvetica = loadFont("fonts/inter.ttf");

        PdfFormXObject layer2 = signatureAppearance.getLayer2();
        PdfCanvas canvas = new PdfCanvas(layer2, pdfDocument);

        Rectangle signatureRect = new Rectangle(0, 0, signaturePositionOnPage.getWidth(),
                signaturePositionOnPage.getHeight());

        Div textDiv = new Div();
        textDiv.setHeight(signatureRect.getHeight());
        textDiv.setWidth(signatureRect.getWidth());
        textDiv.setVerticalAlignment(VerticalAlignment.MIDDLE);
        textDiv.setHorizontalAlignment(HorizontalAlignment.LEFT);

        Text contenido = new Text(nombreFirmante.trim());
        Paragraph paragraph = new Paragraph().add(contenido).setFont(fontHelvetica).setMargin(0)
                .setMultipliedLeading(0.8f).
                setFontSize(4.75f);
        textDiv.add(paragraph);

        contenido = new Text("Validar únicamente con Nexus Soluciones");
        paragraph = new Paragraph().add(contenido).setFont(fontHelvetica).setMargin(0).
                setMultipliedLeading(0.9f)
                .setFontSize(2.75f);
        textDiv.add(paragraph);

        contenido = new Text("Fecha: " + signTime);
        paragraph = new Paragraph().add(contenido).setFont(fontHelvetica).setMargin(0).
                setMultipliedLeading(0.9f)
                .setFontSize(2.75f);
        textDiv.add(paragraph);

        contenido = new Text("Razón: " + reason);
        paragraph = new Paragraph().add(contenido).setFont(fontHelvetica).setMargin(0).
                setMultipliedLeading(0.9f)
                .setFontSize(2.75f);
        textDiv.add(paragraph);

        contenido = new Text("Localización: " + location);
        paragraph = new Paragraph().add(contenido).setFont(fontHelvetica).setMargin(0).
                setMultipliedLeading(0.9f)
                .setFontSize(2.75f);
        textDiv.add(paragraph);

        contenido = new Text("Nombre de reconocimiento " + informacionCertificado.trim());
        paragraph = new Paragraph().add(contenido).setFont(fontHelvetica).setMargin(0).
                setMultipliedLeading(0.8f)
                .setFontSize(2.50f);
        textDiv.add(paragraph);

        try (Canvas textLayoutCanvas = new Canvas(canvas, signatureRect)) {
            textLayoutCanvas.add(textDiv);
        }
    }
}
