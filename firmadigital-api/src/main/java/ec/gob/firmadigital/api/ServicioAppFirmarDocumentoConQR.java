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
import ec.gob.firmadigital.libreria.sign.DigestAlgorithm;
import ec.gob.firmadigital.libreria.sign.PrivateKeySigner;
import ec.gob.firmadigital.libreria.sign.pdf.PadesBasic;
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
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ec.gob.firmadigital.libreria.utils.Utils.loadFont;

/**
 * REST Web Service para firmar documentos PDF con estampado QR previo.
 * Este servicio primero agrega un estampado visual con QR al documento
 * y luego lo firma digitalmente.
 *
@Path("/appfirmardocumentoconqr")
public class ServicioAppFirmarDocumentoConQR extends RequestSizeFilter {

    private static final Logger LOGGER = Logger.getLogger(ServicioAppFirmarDocumentoConQR.class.getName());

    /**
     * Firma un documento PDF agregando primero un estampado visual con QR.
     * 
     * @param pkcs12Base64 Certificado PKCS#12 codificado en Base64
     * @param password Contraseña del certificado
     * @param documentoBase64 Documento PDF codificado en Base64
     * @param jsonMetadata JSON con metadatos:
     *   Metadatos de firma:
     *   - razon: Razón de la firma (opcional, default: "Firma digital")
     *   - localizacion: Ubicación de la firma (opcional, default: "Ecuador")
     *   - cargo: Cargo del firmante (opcional)
     *   
     *   Metadatos del estampado QR:
     *   - infoQR: Información adicional para el QR (opcional, ej: URL de verificación)
     *   - qrPagina: Número de página donde estampar (opcional, default: última página)
     *   - qrPosX: Posición X del estampado (opcional, default: 50)
     *   - qrPosY: Posición Y del estampado (opcional, default: 50)
     *   - qrAncho: Ancho del estampado (opcional, default: 200)
     *   - qrAlto: Alto del estampado (opcional, default: 100)
     * @return JSON con el documento firmado y estampado en Base64
     */
    @POST
    @Secured
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String firmarDocumentoConQR(
            @FormParam("pkcs12") String pkcs12Base64,
            @FormParam("password") String password,
            @FormParam("documento") String documentoBase64,
            @FormParam("json") String jsonMetadata
    ) {
        LOGGER.log(Level.INFO, "Iniciando proceso de firma de documento con estampado QR");
        
        try {
            // Validar parámetros requeridos
            if (pkcs12Base64 == null || pkcs12Base64.isEmpty()) {
                return crearRespuestaError("El certificado PKCS#12 es requerido");
            }
            if (password == null || password.isEmpty()) {
                return crearRespuestaError("La contraseña del certificado es requerida");
            }
            if (documentoBase64 == null || documentoBase64.isEmpty()) {
                return crearRespuestaError("El documento a firmar es requerido");
            }
            
            // 1. Decodificar certificado PKCS#12
            LOGGER.log(Level.INFO, "Decodificando certificado PKCS#12");
            byte[] certBytes = decodificarBase64(pkcs12Base64);
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new ByteArrayInputStream(certBytes), password.toCharArray());
            
            // 2. Obtener llave privada y cadena de certificados
            LOGGER.log(Level.INFO, "Obteniendo llave privada y certificados");
            String alias = keyStore.aliases().nextElement();
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
            Certificate[] certChain = keyStore.getCertificateChain(alias);
            
            if (privateKey == null) {
                return crearRespuestaError("No se pudo obtener la llave privada del certificado");
            }
            if (certChain == null || certChain.length == 0) {
                return crearRespuestaError("No se pudo obtener la cadena de certificados");
            }
            
            // Extraer nombre del firmante del certificado
            String nombreFirmante = extraerNombreFirmante(certChain[0]);
            
            // 3. Decodificar documento
            LOGGER.log(Level.INFO, "Decodificando documento PDF");
            byte[] docBytes = decodificarBase64(documentoBase64);
            
            // 4. Parsear metadatos (si existen)
            Properties params = new Properties();
            String razon = "Firma digital";
            String localizacion = "Ecuador";
            String infoQR = "";
            int qrPagina = -1; // -1 = última página
            float qrPosX = 50f;
            float qrPosY = 50f;
            float qrAncho = 200f;
            float qrAlto = 100f;
            
            if (jsonMetadata != null && !jsonMetadata.isEmpty()) {
                try {
                    JsonObject metadata = new Gson().fromJson(jsonMetadata, JsonObject.class);
                    
                    // Metadatos de firma
                    if (metadata.has("razon")) {
                        razon = metadata.get("razon").getAsString();
                        params.setProperty("razon", razon);
                    }
                    if (metadata.has("localizacion")) {
                        localizacion = metadata.get("localizacion").getAsString();
                        params.setProperty("localizacion", localizacion);
                    }
                    if (metadata.has("cargo")) {
                        params.setProperty("cargo", metadata.get("cargo").getAsString());
                    }
                    
                    // Metadatos de estampado QR
                    if (metadata.has("infoQR")) {
                        infoQR = metadata.get("infoQR").getAsString();
                    }
                    if (metadata.has("qrPagina")) {
                        qrPagina = metadata.get("qrPagina").getAsInt();
                    }
                    if (metadata.has("qrPosX")) {
                        qrPosX = metadata.get("qrPosX").getAsFloat();
                    }
                    if (metadata.has("qrPosY")) {
                        qrPosY = metadata.get("qrPosY").getAsFloat();
                    }
                    if (metadata.has("qrAncho")) {
                        qrAncho = metadata.get("qrAncho").getAsFloat();
                    }
                    if (metadata.has("qrAlto")) {
                        qrAlto = metadata.get("qrAlto").getAsFloat();
                    }
                    
                    LOGGER.log(Level.INFO, "Metadatos procesados: {0}", metadata);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error al parsear metadatos JSON, se usarán valores por defecto: {0}", e.getMessage());
                }
            }
            
            // 5. PRIMERO: Agregar estampado visual con QR
            LOGGER.log(Level.INFO, "Agregando estampado visual con QR al documento");
            byte[] documentoEstampado = agregarEstampadoQR(
                docBytes, 
                nombreFirmante, 
                razon, 
                localizacion, 
                infoQR,
                qrPagina,
                qrPosX,
                qrPosY,
                qrAncho,
                qrAlto
            );
            
            // 6. SEGUNDO: Firmar digitalmente el documento estampado
            LOGGER.log(Level.INFO, "Iniciando firma digital del documento estampado");
            PrivateKeySigner signer = new PrivateKeySigner(privateKey, DigestAlgorithm.SHA256);
            PadesBasic padesSigner = new PadesBasic(signer);
            
            // Convertir byte[] a InputStream para el método sign
            ByteArrayInputStream inputStream = new ByteArrayInputStream(documentoEstampado);
            byte[] documentoFirmado = padesSigner.sign(inputStream, signer, certChain, params);
            
            // 7. Codificar y retornar
            String documentoFirmadoBase64 = Base64.getEncoder().encodeToString(documentoFirmado);
            LOGGER.log(Level.INFO, "Documento estampado y firmado exitosamente");
            
            JsonObject response = new JsonObject();
            response.addProperty("resultado", "OK");
            response.addProperty("mensaje", "Documento estampado con QR y firmado digitalmente exitosamente");
            response.addProperty("documentoFirmado", documentoFirmadoBase64);
            
            return new Gson().toJson(response);
            
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Error de formato en los datos: {0}", e.getMessage());
            return crearRespuestaError("Error de formato: " + e.getMessage());
        } catch (java.security.UnrecoverableKeyException e) {
            LOGGER.log(Level.SEVERE, "Contraseña incorrecta: {0}", e.getMessage());
            return crearRespuestaError("Contraseña del certificado incorrecta");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al procesar documento: {0}", e);
            return crearRespuestaError("Error al procesar documento: " + e.getMessage());
        }
    }
    
    /**
     * Agrega un estampado visual con QR al documento PDF.
     */
    private byte[] agregarEstampadoQR( usando la clase QrAppereance de la librería.
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
        
        LOGGER.log(Level.INFO, "Total de páginas: {0}, Página destino para QR: {1}", new Object[]{totalPaginas, paginaDestino});
        
        // Generar fecha/hora actual
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String fechaHora = sdf.format(new Date());
        
        // Crear instancia de QrAppereance de la librería
        QrAppereance qrAppearance = new QrAppereance(nombreFirmante, razon, localizacion, fechaHora, infoQR);
        
        // Crear un PdfSigner temporal para obtener PdfSignatureAppearance
        // Cerramos el documento anterior y abrimos uno nuevo con PdfSigner
        pdfDoc.close();
        
        // Reabrir para usar con PdfSigner
        ByteArrayInputStream signInputStream = new ByteArrayInputStream(pdfBytes);
        ByteArrayOutputStream signOutputStream = new ByteArrayOutputStream();
        
        PdfReader signReader = new PdfReader(signInputStream);
        PdfSigner pdfSigner = new PdfSigner(signReader, signOutputStream, false);
        
        // Configurar la apariencia visual
        PdfSignatureAppearance appearance = pdfSigner.getSignatureAppearance();
        Rectangle rect = new Rectangle(posX, posY, ancho, alto);
        
        // Usar la clase QrAppereance de la librería para crear la apariencia
        PdfDocument signPdfDoc = pdfSigner.getDocument();
        qrAppearance.createCustomAppearance(appearance, paginaDestino, signPdfDoc, rect);
        
        // Crear un FormXObject con la apariencia y agregarlo al documento
        // En lugar de firmar, solo agregamos el visual
        PdfFormXObject form = appearance.getLayer2();
        
        // Cerrar el pdfSigner sin firmar y obtener el documento con la apariencia
        signPdfDoc.close();
        
        LOGGER.log(Level.INFO, "Estampado QR agregado en página {0} en posición ({1}, {2})", 
                   new Object[]{paginaDestino, posX, posY});
        
        return signO
    /**
     * Extrae el nombre común (CN) del certificado.
     */
    private String extraerNombreFirmante(Certificate cert) {
        try {
            if (cert instanceof X509Certificate) {
                X509Certificate x509cert = (X509Certificate) cert;
                String dn = x509cert.getSubjectDN().getName();
                
                // Buscar CN= en el DN
                String[] parts = dn.split(",");
                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("CN=")) {
                        return part.substring(3).trim();
                    }
                }
                
                // Si no encuentra CN, retornar el DN completo
                return dn;
            }
            return "Firmante desconocido";
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al extraer nombre del firmante: {0}", e.getMessage());
            return "Firmante desconocido";
        }
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
