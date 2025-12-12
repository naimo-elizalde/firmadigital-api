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
package ec.gob.firmadigital.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import ec.gob.firmadigital.api.security.Secured;
import ec.gob.firmadigital.libreria.utils.QRCode;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ec.gob.firmadigital.libreria.utils.Utils.loadFont;

/**
 * REST Web Service para agregar estampado visual con QR a documentos PDF.
 * Este servicio no firma digitalmente el documento, solo agrega una marca visual
 * con código QR que puede contener información del documento o una URL de verificación.
 *
 * @author Luis González
 */
@Path("/appestampadoqr")
public class ServicioAppEstampadoQR extends RequestSizeFilter {

    private static final Logger LOGGER = Logger.getLogger(ServicioAppEstampadoQR.class.getName());

    /**
     * Agrega un estampado visual con QR a un documento PDF.
     * 
     * @param documentoBase64 Documento PDF codificado en Base64
     * @param jsonMetadata JSON con metadatos del estampado:
     *   - nombreFirmante: Nombre de la persona que estampa (requerido)
     *   - razon: Razón del estampado (opcional, default: "Documento verificado")
     *   - localizacion: Ubicación del estampado (opcional, default: "Ecuador")
     *   - infoQR: Información adicional para el QR (opcional, ej: URL de verificación)
     *   - pagina: Número de página donde estampar (opcional, default: última página)
     *   - posX: Posición X del estampado (opcional, default: 50)
     *   - posY: Posición Y del estampado (opcional, default: 50)
     *   - ancho: Ancho del estampado (opcional, default: 200)
     *   - alto: Alto del estampado (opcional, default: 100)
     * @return JSON con el documento estampado en Base64
     */
    @POST
    @Secured
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String estamparQR(
            @FormParam("documento") String documentoBase64,
            @FormParam("json") String jsonMetadata
    ) {
        LOGGER.log(Level.INFO, "Iniciando proceso de estampado con QR");
        
        try {
            // Validar parámetros requeridos
            if (documentoBase64 == null || documentoBase64.isEmpty()) {
                return crearRespuestaError("El documento PDF es requerido");
            }
            if (jsonMetadata == null || jsonMetadata.isEmpty()) {
                return crearRespuestaError("Los metadatos JSON son requeridos");
            }
            
            // 1. Parsear metadatos
            JsonObject metadata = new Gson().fromJson(jsonMetadata, JsonObject.class);
            
            // Validar nombreFirmante
            if (!metadata.has("nombreFirmante") || metadata.get("nombreFirmante").getAsString().isEmpty()) {
                return crearRespuestaError("El nombre del firmante es requerido en los metadatos");
            }
            
            String nombreFirmante = metadata.get("nombreFirmante").getAsString();
            String razon = metadata.has("razon") ? metadata.get("razon").getAsString() : "Documento verificado";
            String localizacion = metadata.has("localizacion") ? metadata.get("localizacion").getAsString() : "Ecuador";
            String infoQR = metadata.has("infoQR") ? metadata.get("infoQR").getAsString() : "";
            
            // Parámetros de posición y tamaño
            int pagina = metadata.has("pagina") ? metadata.get("pagina").getAsInt() : -1; // -1 = última página
            float posX = metadata.has("posX") ? metadata.get("posX").getAsFloat() : 50f;
            float posY = metadata.has("posY") ? metadata.get("posY").getAsFloat() : 50f;
            float ancho = metadata.has("ancho") ? metadata.get("ancho").getAsFloat() : 200f;
            float alto = metadata.has("alto") ? metadata.get("alto").getAsFloat() : 100f;
            
            // 2. Decodificar documento
            LOGGER.log(Level.INFO, "Decodificando documento PDF");
            byte[] docBytes = decodificarBase64(documentoBase64);
            
            // 3. Agregar estampado visual con QR
            LOGGER.log(Level.INFO, "Agregando estampado visual con QR");
            byte[] documentoEstampado = agregarEstampadoQR(
                docBytes, 
                nombreFirmante, 
                razon, 
                localizacion, 
                infoQR,
                pagina,
                posX,
                posY,
                ancho,
                alto
            );
            
            // 4. Codificar y retornar
            String documentoEstampadoBase64 = Base64.getEncoder().encodeToString(documentoEstampado);
            LOGGER.log(Level.INFO, "Documento estampado exitosamente");
            
            JsonObject response = new JsonObject();
            response.addProperty("resultado", "OK");
            response.addProperty("mensaje", "Estampado QR agregado exitosamente");
            response.addProperty("documentoEstampado", documentoEstampadoBase64);
            
            return new Gson().toJson(response);
            
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Error de formato en los datos: {0}", e.getMessage());
            return crearRespuestaError("Error de formato: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al agregar estampado QR: {0}", e);
            return crearRespuestaError("Error al agregar estampado QR: " + e.getMessage());
        }
    }
    
    /**
     * Agrega un estampado visual con QR al documento PDF.
     */
    private byte[] agregarEstampadoQR(
            byte[] pdfBytes,
            String nombreFirmante,
            String razon,
            String localizacion,
            String infoQR,
            int numeroPagina,
            float posX,
            float posY,
            float ancho,
            float alto
    ) throws Exception {
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfBytes);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        PdfReader reader = new PdfReader(inputStream);
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDoc = new PdfDocument(reader, writer);
        
        // Determinar página donde agregar el estampado
        int totalPaginas = pdfDoc.getNumberOfPages();
        int paginaDestino = (numeroPagina <= 0 || numeroPagina > totalPaginas) ? totalPaginas : numeroPagina;
        
        LOGGER.log(Level.INFO, "Total de páginas: {0}, Página destino: {1}", new Object[]{totalPaginas, paginaDestino});
        
        // Generar fecha/hora actual
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String fechaHora = sdf.format(new Date());
        
        // Obtener la página donde se agregará el estampado
        PdfPage page = pdfDoc.getPage(paginaDestino);
        PdfCanvas pdfCanvas = new PdfCanvas(page);
        
        // Generar código QR usando la utilidad de la librería
        String textoQR = "FIRMADO POR: " + nombreFirmante.trim() + "\n";
        textoQR = textoQR + "RAZON: " + razon + "\n";
        textoQR = textoQR + "LOCALIZACION: " + localizacion + "\n";
        textoQR = textoQR + "FECHA: " + fechaHora + "\n";
        if (infoQR != null && !infoQR.isEmpty()) {
            textoQR = textoQR + infoQR;
        }
        
        byte[] byteQR = QRCode.generateQR(textoQR, (int)alto, (int)alto);
        
        // Cargar fuentes
        PdfFont fontCourier = loadFont("fonts/courier.ttf");
        PdfFont fontCourierBold = loadFont("fonts/courier-bold.ttf");
        
        // Crear el área para el QR (cuadrado con el alto)
        float qrWidth = alto;  // El QR es cuadrado
        Rectangle qrRect = new Rectangle(posX, posY, qrWidth, alto);
        
        // Crear el área para el texto (resto del ancho)
        float textX = posX + qrWidth;
        float textWidth = ancho - qrWidth;
        Rectangle textRect = new Rectangle(textX, posY, textWidth, alto);
        
        // Agregar imagen QR
        try (Canvas qrCanvas = new Canvas(pdfCanvas, qrRect)) {
            Div imageDiv = new Div();
            imageDiv.setHeight(alto);
            imageDiv.setWidth(qrWidth);
            imageDiv.setVerticalAlignment(VerticalAlignment.MIDDLE);
            imageDiv.setHorizontalAlignment(HorizontalAlignment.CENTER);
            
            Image image = new Image(ImageDataFactory.create(byteQR));
            image.setAutoScale(true);
            imageDiv.add(image);
            
            qrCanvas.add(imageDiv);
        }
        
        // Agregar texto informativo
        try (Canvas textCanvas = new Canvas(pdfCanvas, textRect)) {
            Div textDiv = new Div();
            textDiv.setHeight(alto);
            textDiv.setWidth(textWidth);
            textDiv.setVerticalAlignment(VerticalAlignment.MIDDLE);
            textDiv.setHorizontalAlignment(HorizontalAlignment.LEFT);
            
            Text texto = new Text("Firmado electrónicamente por:\n");
            Paragraph paragraph = new Paragraph().add(texto).setFont(fontCourier).setMargin(0)
                    .setMultipliedLeading(0.9f).setFontSize(3.25f);
            textDiv.add(paragraph);
            
            Text contenido = new Text(nombreFirmante.trim());
            paragraph = new Paragraph().add(contenido).setFont(fontCourierBold).setMargin(0)
                    .setMultipliedLeading(0.9f).setFontSize(6.25f);
            textDiv.add(paragraph);
            
            Text info = new Text("\nValidar únicamente con Nexus Soluciones");
            paragraph = new Paragraph().add(info).setFont(fontCourier).setMargin(0)
                    .setMultipliedLeading(0.9f).setFontSize(3.25f);
            textDiv.add(paragraph);
            
            textCanvas.add(textDiv);
        }
        
        // Cerrar documento
        pdfDoc.close();
        
        LOGGER.log(Level.INFO, "Estampado QR agregado en página {0} en posición ({1}, {2})", 
                   new Object[]{paginaDestino, posX, posY});
        
        return outputStream.toByteArray();
    }
    
    private String crearRespuestaError(String mensaje) {
        JsonObject error = new JsonObject();
        error.addProperty("resultado", "ERROR");
        error.addProperty("mensaje", mensaje);
        return new Gson().toJson(error);
    }
    
    /**
     * Decodifica una cadena Base64 limpiando caracteres no válidos.
     * Elimina espacios, saltos de línea y otros caracteres no Base64.
     */
    private byte[] decodificarBase64(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            throw new IllegalArgumentException("Cadena Base64 vacía");
        }
        
        // Log de la longitud original para debugging
        LOGGER.log(Level.INFO, "Longitud Base64 original: {0}", base64String.length());
        
        // Limpiar la cadena: eliminar espacios, saltos de línea, retornos de carro, tabulaciones
        String cleaned = base64String.trim().replaceAll("\\s+", "");
        
        LOGGER.log(Level.INFO, "Longitud Base64 después de limpiar: {0}", cleaned.length());
        
        try {
            // Intentar con decoder estándar primero
            return Base64.getDecoder().decode(cleaned);
        } catch (IllegalArgumentException e1) {
            LOGGER.log(Level.WARNING, "Fallo decodificación estándar, intentando con MIME decoder: {0}", e1.getMessage());
            
            try {
                // Intentar con MIME decoder que es más permisivo
                return Base64.getMimeDecoder().decode(cleaned);
            } catch (IllegalArgumentException e2) {
                LOGGER.log(Level.SEVERE, "Error al decodificar Base64 con ambos decoders: {0}", e2.getMessage());
                throw new IllegalArgumentException("El contenido Base64 no es válido. Verifique que el contenido esté correctamente codificado.");
            }
        }
    }
}
