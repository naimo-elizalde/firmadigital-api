# 🚀 SOLUCIÓN COMPLETA - API Standalone

## ✅ Cambios Realizados

### 1. Dependencia Agregada al pom.xml

La API ahora incluye la librería de firma digital:

```xml
<dependency>
    <groupId>ec.gob.firmadigital</groupId>
    <artifactId>libreria</artifactId>
    <version>4.1.0-SNAPSHOT</version>
</dependency>
```

### 2. Servicios Modificados (Sin Llamados Externos)

✅ **ServicioVersion.java** - Funciona localmente  
✅ **ServicioFechaHora.java** - Funciona localmente  
🔄 **Otros servicios** - Requieren implementación completa con la librería

---

## 📋 Plan de Implementación Completa

### Arquitectura Actual vs Nueva

**ANTES (Con servicios externos):**
```
Cliente → API → REST Call → Servicio Externo → Librería
                    ❌ FALLA AQUÍ
```

**AHORA (Standalone):**
```
Cliente → API → Librería Directamente ✅
```

---

## 🎯 Servicios a Implementar

### 1. ServicioAppFirmarDocumento - PRIORITARIO ⭐

**Archivo:** `firmadigital-api/src/main/java/ec/gob/firmadigital/api/ServicioAppFirmarDocumento.java`

**Implementación Sugerida:**

```java
package ec.gob.firmadigital.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import ec.gob.firmadigital.libreria.sign.DigestAlgorithm;
import ec.gob.firmadigital.libreria.sign.PrivateKeySigner;
import ec.gob.firmadigital.libreria.sign.pdf.PadesBasic;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.Properties;

@Path("/appfirmardocumento")
public class ServicioAppFirmarDocumento {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String firmarDocumento(
            @FormParam("jwt") String jwt,
            @FormParam("pkcs12") String pkcs12Base64,
            @FormParam("password") String password,
            @FormParam("documento") String documentoBase64,
            @FormParam("json") String jsonMetadata,
            @FormParam("base64") String base64Version
    ) {
        try {
            // 1. Decodificar certificado PKCS#12
            byte[] certBytes = Base64.getDecoder().decode(pkcs12Base64);
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new ByteArrayInputStream(certBytes), password.toCharArray());
            
            // 2. Obtener llave privada y cadena de certificados
            String alias = keyStore.aliases().nextElement();
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
            Certificate[] certChain = keyStore.getCertificateChain(alias);
            
            // 3. Decodificar documento
            byte[] docBytes = Base64.getDecoder().decode(documentoBase64);
            
            // 4. Parsear metadatos (si existen)
            Properties params = new Properties();
            if (jsonMetadata != null && !jsonMetadata.isEmpty()) {
                JsonObject metadata = new Gson().fromJson(jsonMetadata, JsonObject.class);
                if (metadata.has("razon")) params.setProperty("razon", metadata.get("razon").getAsString());
                if (metadata.has("localizacion")) params.setProperty("localizacion", metadata.get("localizacion").getAsString());
                if (metadata.has("cargo")) params.setProperty("cargo", metadata.get("cargo").getAsString());
            }
            
            // 5. Crear firmador y firmar documento
            PrivateKeySigner signer = new PrivateKeySigner(privateKey, DigestAlgorithm.SHA256);
            PadesBasic padesSigner = new PadesBasic(signer);
            byte[] documentoFirmado = padesSigner.sign(docBytes, certChain, params);
            
            // 6. Codificar y retornar
            String documentoFirmadoBase64 = Base64.getEncoder().encodeToString(documentoFirmado);
            
            JsonObject response = new JsonObject();
            response.addProperty("resultado", "OK");
            response.addProperty("mensaje", "Documento firmado exitosamente");
            response.addProperty("documentoFirmado", documentoFirmadoBase64);
            
            return new Gson().toJson(response);
            
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("resultado", "ERROR");
            error.addProperty("mensaje", "Error al firmar documento: " + e.getMessage());
            error.addProperty("detalle", e.getClass().getName());
            return new Gson().toJson(error);
        }
    }
}
```

---

### 2. ServicioAppValidarCertificadoDigital

**Archivo:** `firmadigital-api/src/main/java/ec/gob/firmadigital/api/ServicioAppValidarCertificadoDigital.java`

**Implementación Sugerida:**

```java
package ec.gob.firmadigital.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

@Path("/appvalidarcertificadodigital")
public class ServicioAppValidarCertificadoDigital {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String validarCertificado(
            @FormParam("jwt") String jwt,
            @FormParam("pkcs12") String pkcs12Base64,
            @FormParam("password") String password,
            @FormParam("base64") String base64Version
    ) {
        try {
            // 1. Decodificar certificado PKCS#12
            byte[] certBytes = Base64.getDecoder().decode(pkcs12Base64);
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new ByteArrayInputStream(certBytes), password.toCharArray());
            
            // 2. Obtener certificado
            String alias = keyStore.aliases().nextElement();
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
            
            // 3. Verificar validez
            boolean valido = true;
            String estado = "VIGENTE";
            try {
                cert.checkValidity();
            } catch (Exception e) {
                valido = false;
                if (new Date().before(cert.getNotBefore())) {
                    estado = "NO_VIGENTE_AUN";
                } else {
                    estado = "EXPIRADO";
                }
            }
            
            // 4. Extraer información
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            
            JsonObject response = new JsonObject();
            response.addProperty("resultado", "OK");
            response.addProperty("valido", valido);
            response.addProperty("subject", cert.getSubjectDN().toString());
            response.addProperty("issuer", cert.getIssuerDN().toString());
            response.addProperty("serialNumber", cert.getSerialNumber().toString());
            response.addProperty("notBefore", sdf.format(cert.getNotBefore()));
            response.addProperty("notAfter", sdf.format(cert.getNotAfter()));
            response.addProperty("estado", estado);
            
            return new Gson().toJson(response);
            
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("resultado", "ERROR");
            error.addProperty("mensaje", "Error al validar certificado: " + e.getMessage());
            return new Gson().toJson(error);
        }
    }
}
```

---

### 3. ServicioAppVerificarDocumento

**Archivo:** `firmadigital-api/src/main/java/ec/gob/firmadigital/api/ServicioAppVerificarDocumento.java`

**Implementación Sugerida:**

```java
package ec.gob.firmadigital.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ec.gob.firmadigital.libreria.sign.SignInfo;
import ec.gob.firmadigital.libreria.sign.pdf.PadesSigner;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.List;

@Path("/appverificardocumento")
public class ServicioAppVerificarDocumento {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String verificarDocumento(
            @FormParam("jwt") String jwt,
            @FormParam("documento") String documentoBase64,
            @FormParam("base64") String base64Version
    ) {
        try {
            // 1. Decodificar documento
            byte[] docBytes = Base64.getDecoder().decode(documentoBase64);
            
            // 2. Verificar firmas (asumiendo PDF)
            PadesSigner padesSigner = new PadesSigner();
            List<SignInfo> firmas = padesSigner.getSigners(docBytes);
            
            // 3. Construir respuesta
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            
            JsonObject response = new JsonObject();
            response.addProperty("resultado", "OK");
            response.addProperty("firmaValida", !firmas.isEmpty());
            response.addProperty("numeroFirmas", firmas.size());
            
            JsonArray firmasArray = new JsonArray();
            for (SignInfo signInfo : firmas) {
                X509Certificate cert = signInfo.getCertificate();
                
                JsonObject firmaObj = new JsonObject();
                firmaObj.addProperty("firmante", cert.getSubjectDN().toString());
                firmaObj.addProperty("fechaFirma", sdf.format(signInfo.getSigningTime()));
                
                // Verificar validez del certificado
                boolean certValido = true;
                try {
                    cert.checkValidity(signInfo.getSigningTime());
                } catch (Exception e) {
                    certValido = false;
                }
                firmaObj.addProperty("certificadoValido", certValido);
                
                firmasArray.add(firmaObj);
            }
            
            response.add("firmas", firmasArray);
            
            return new Gson().toJson(response);
            
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("resultado", "ERROR");
            error.addProperty("mensaje", "Error al verificar documento: " + e.getMessage());
            return new Gson().toJson(error);
        }
    }
}
```

---

## 🔧 Próximos Pasos

### Paso 1: Verificar los Cambios
Los archivos ya han sido modificados:
1. ✅ `pom.xml` - Dependencia agregada
2. ✅ `ServicioVersion.java` - Standalone
3. ✅ `ServicioFechaHora.java` - Standalone

### Paso 2: Implementar Servicios Restantes
Necesitas reemplazar el contenido de estos archivos con las implementaciones sugeridas arriba:
1. 🔄 `ServicioAppFirmarDocumento.java`
2. 🔄 `ServicioAppValidarCertificadoDigital.java`
3. 🔄 `ServicioAppVerificarDocumento.java`

### Paso 3: Reconstruir Docker
```bash
cd /Users/lgonzalez/Projects/nexus-soluciones/firmador
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

### Paso 4: Probar
```bash
# Test 1: Versión
curl -X POST http://localhost:8080/api/version \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "base64=NDQuMS4w"

# Test 2: Fecha y Hora
curl -X POST http://localhost:8080/api/fecha-hora \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "base64=NDQuMS4w"

# Test 3: Validar Certificado (requiere archivo .p12)
curl -X POST http://localhost:8080/api/appvalidarcertificadodigital \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "jwt=test" \
  -d "pkcs12=$(cat certificado.p12 | base64 -w 0)" \
  -d "password=MiContraseña" \
  -d "base64=NDQuMS4w"
```

---

## ⚠️ Consideraciones Importantes

### 1. Tamaño de Archivos
Los archivos PDF y certificados pueden ser grandes. Verifica los límites de:
- WildFly (configuración del servidor)
- Nginx/reverse proxy si aplica
- Cliente HTTP

### 2. Performance
Las operaciones criptográficas son intensivas en CPU:
- Considera timeouts apropiados
- Implementa caché si es necesario
- Monitorea uso de recursos

### 3. Seguridad
- Las contraseñas de certificados se transmiten en texto plano
- **SIEMPRE usa HTTPS en producción**
- Considera implementar rate limiting
- Valida y sanitiza todas las entradas

### 4. Logs
Agrega logs apropiados para debugging:
```java
private static final Logger LOGGER = Logger.getLogger(ServicioAppFirmarDocumento.class.getName());
LOGGER.log(Level.INFO, "Firmando documento...");
```

---

## 📊 Estado Actual

| Servicio | Estado | Requiere Archivos | Complejidad |
|----------|--------|-------------------|-------------|
| ServicioVersion | ✅ Completo | No | Baja |
| ServicioFechaHora | ✅ Completo | No | Baja |
| ServicioAppFirmarDocumento | 🔄 Pendiente | Sí (.p12, .pdf) | Alta |
| ServicioAppValidarCertificadoDigital | 🔄 Pendiente | Sí (.p12) | Media |
| ServicioAppVerificarDocumento | 🔄 Pendiente | Sí (.pdf firmado) | Media |
| ServicioCertificado | ⏳ No prioritario | OCSP/CRL | Alta |
| ServicioFirmaDigital | ⏳ No prioritario | Depende | Media |
| ServicioJWT | ⏳ No prioritario | Depende | Baja |

---

## 🎓 Aprendizajes

1. **Arquitectura Proxy vs Standalone:**
   - Proxy: Más flexible pero dependiente
   - Standalone: Más robusto pero más complejo

2. **Firma Digital:**
   - Requiere KeyStore (PKCS#12)
   - Cadena de certificados completa
   - Algoritmos específicos (SHA256, RSA)

3. **iText y DSS:**
   - Librerías maduras para PDF y XML
   - API compleja pero poderosa
   - Bien documentadas

---

## 📚 Referencias Útiles

- **Librería Local:** `/firmadigital-libreria/src/main/java/ec/gob/firmadigital/libreria/`
- **Tests:** `/firmadigital-libreria/src/test/java/`
- **iText Docs:** https://itextpdf.com/en/resources/api-documentation
- **DSS Docs:** https://ec.europa.eu/digital-building-blocks/sites/display/DIGITAL/Digital+Signature+Service+-++DSS

---

**¿Necesitas ayuda para implementar estos cambios?** Puedo:
1. Modificar los archivos específicos uno por uno
2. Crear servicios de prueba adicionales
3. Agregar más validaciones y manejo de errores
4. Actualizar la documentación de la API
