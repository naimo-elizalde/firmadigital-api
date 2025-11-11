# Implementación Standalone - Sin Servicios Externos

## 🎯 Objetivo

Modificar todos los servicios de la API para que usen directamente la librería `firmadigital-libreria` en lugar de hacer llamados REST a servicios externos.

## 📋 Estado de Implementación

### ✅ Completados
1. **ServicioVersion** - Lee versión del archivo properties local
2. **ServicioFechaHora** - Retorna fecha/hora del servidor directamente

### 🔄 En Proceso
3. **ServicioAppFirmarDocumento** - Firmar documentos PDF
4. **ServicioAppValidarCertificadoDigital** - Validar certificados
5. **ServicioAppVerificarDocumento** - Verificar documentos firmados

### ⏳ Pendientes
6. **ServicioAppFirmarDocumentoTransversal** - Firma transversal
7. **ServicioCertificado** - Verificar revocación (requiere CRL/OCSP)
8. **ServicioFirmaDigital** - Operaciones con documentos
9. **ServicioJWT** - Validación JWT
10. **ServicioApiUrl** - Validación de URLs

## 🔧 Cambios Técnicos

### 1. Agregar Dependencia de la Librería

Se agregó al `pom.xml` de la API:
```xml
<dependency>
    <groupId>ec.gob.firmadigital</groupId>
    <artifactId>libreria</artifactId>
    <version>4.1.0-SNAPSHOT</version>
</dependency>
```

### 2. Clases Principales de la Librería

#### Para Firmar PDF:
```java
import ec.gob.firmadigital.libreria.sign.pdf.PadesSigner;
import ec.gob.firmadigital.libreria.sign.pdf.PadesBasic;
```

#### Para Firmar XML:
```java
import ec.gob.firmadigital.libreria.sign.xades.XAdESSigner;
```

#### Para Validar Certificados:
```java
import ec.gob.firmadigital.libreria.utils.CertificateUtils;
import ec.gob.firmadigital.libreria.ocsp.ValidadorOCSP;
import ec.gob.firmadigital.libreria.crl.ServicioCRL;
```

#### Para Verificar Firmas:
```java
import ec.gob.firmadigital.libreria.sign.Validator;
import ec.gob.firmadigital.libreria.sign.SignInfo;
import ec.gob.firmadigital.libreria.sign.pdf.PadesSigner;
```

## 📝 Plan de Implementación

### Fase 1: Firma de Documentos (PRIORITARIO)

#### ServicioAppFirmarDocumento
**Entrada:**
- `pkcs12` (String base64): Certificado PKCS#12
- `password` (String): Contraseña del certificado
- `documento` (String base64): Documento PDF a firmar
- `json` (String): Metadatos de firma (razón, localización, cargo)

**Proceso:**
1. Decodificar certificado y documento de base64
2. Cargar KeyStore del PKCS#12
3. Usar `PadesBasic` o `PadesSigner` para firmar
4. Retornar documento firmado en base64

**Código de referencia:**
```java
// Decodificar certificado
byte[] certBytes = Base64.getDecoder().decode(pkcs12Base64);
KeyStore keyStore = KeyStore.getInstance("PKCS12");
keyStore.load(new ByteArrayInputStream(certBytes), password.toCharArray());

// Decodificar documento
byte[] docBytes = Base64.getDecoder().decode(documentoBase64);

// Parsear metadatos JSON
JsonObject metadata = new Gson().fromJson(json, JsonObject.class);
String razon = metadata.get("razon").getAsString();
String localizacion = metadata.get("localizacion").getAsString();

// Firmar usando la librería
PadesBasic padesSigner = new PadesBasic();
byte[] documentoFirmado = padesSigner.sign(...);

// Retornar en base64
String resultado = Base64.getEncoder().encodeToString(documentoFirmado);
```

### Fase 2: Validación de Certificados

#### ServicioAppValidarCertificadoDigital
**Entrada:**
- `pkcs12` (String base64): Certificado PKCS#12
- `password` (String): Contraseña

**Proceso:**
1. Cargar certificado
2. Verificar validez (fechas, formato)
3. Verificar revocación (OCSP/CRL - opcional)
4. Extraer información del certificado

**Código de referencia:**
```java
byte[] certBytes = Base64.getDecoder().decode(pkcs12Base64);
KeyStore keyStore = KeyStore.getInstance("PKCS12");
keyStore.load(new ByteArrayInputStream(certBytes), password.toCharArray());

String alias = keyStore.aliases().nextElement();
X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);

// Verificar validez
cert.checkValidity();

// Extraer información
JsonObject response = new JsonObject();
response.addProperty("valido", true);
response.addProperty("subject", cert.getSubjectDN().toString());
response.addProperty("issuer", cert.getIssuerDN().toString());
response.addProperty("serialNumber", cert.getSerialNumber().toString());
response.addProperty("notBefore", cert.getNotBefore().toString());
response.addProperty("notAfter", cert.getNotAfter().toString());
```

### Fase 3: Verificación de Documentos Firmados

#### ServicioAppVerificarDocumento
**Entrada:**
- `documento` (String base64): Documento firmado

**Proceso:**
1. Decodificar documento
2. Detectar tipo (PDF o XML)
3. Usar PadesSigner o XAdESSigner para extraer firmas
4. Verificar cada firma
5. Retornar información de firmas

**Código de referencia:**
```java
byte[] docBytes = Base64.getDecoder().decode(documentoBase64);

PadesSigner padesSigner = new PadesSigner();
List<SignInfo> firmas = padesSigner.getSigners(docBytes);

JsonObject response = new JsonObject();
response.addProperty("firmaValida", !firmas.isEmpty());

JsonArray firmasArray = new JsonArray();
for (SignInfo signInfo : firmas) {
    JsonObject firmaObj = new JsonObject();
    X509Certificate cert = signInfo.getCertificate();
    firmaObj.addProperty("firmante", cert.getSubjectDN().toString());
    firmaObj.addProperty("fechaFirma", signInfo.getSigningTime().toString());
    firmasArray.add(firmaObj);
}
response.add("firmas", firmasArray);
```

## 🚧 Consideraciones Importantes

### Manejo de Excepciones
Todas las operaciones deben manejar:
- `CertificadoInvalidoException`
- `InvalidFormatException`
- `IOException`
- `Exception` general

### Validación OCSP/CRL
Para verificación completa de certificados, se requiere:
1. Conexión a servicios OCSP
2. Descarga de CRLs

**Opciones:**
- Implementar validación completa (requiere conectividad)
- Implementar validación básica (solo fechas y formato)
- Hacer validación OCSP/CRL opcional

### Performance
- Operaciones de firma pueden ser lentas
- Considerar timeouts apropiados
- Caché de certificados CA si es necesario

## 📦 Estructura de Respuestas JSON

### Firma Exitosa
```json
{
  "resultado": "OK",
  "mensaje": "Documento firmado exitosamente",
  "documentoFirmado": "<BASE64>"
}
```

### Error
```json
{
  "resultado": "ERROR",
  "mensaje": "Descripción del error",
  "detalle": "Stack trace o información adicional"
}
```

### Validación de Certificado
```json
{
  "resultado": "OK",
  "valido": true,
  "subject": "CN=...",
  "issuer": "CN=...",
  "serialNumber": "123456",
  "notBefore": "2023-01-01T00:00:00Z",
  "notAfter": "2026-01-01T00:00:00Z",
  "estado": "VIGENTE"
}
```

### Verificación de Documento
```json
{
  "resultado": "OK",
  "firmaValida": true,
  "firmas": [
    {
      "firmante": "CN=Juan Pérez...",
      "fechaFirma": "2025-11-11T10:30:00Z",
      "certificadoValido": true
    }
  ]
}
```

## 🔄 Siguiente Pasos

1. ✅ Agregar dependencia de librería al pom.xml
2. 🔄 Implementar ServicioAppFirmarDocumento standalone
3. ⏳ Implementar ServicioAppValidarCertificadoDigital standalone  
4. ⏳ Implementar ServicioAppVerificarDocumento standalone
5. ⏳ Actualizar documentación API-EJEMPLOS.md
6. ⏳ Probar exhaustivamente cada endpoint
7. ⏳ Actualizar Dockerfile si es necesario

## 📚 Referencias

- Librería: `/firmadigital-libreria/src/main/java/ec/gob/firmadigital/libreria/`
- Ejemplos de uso en tests: `/firmadigital-libreria/src/test/java/`
- Documentación iText: https://itextpdf.com/
- DSS (Digital Signature Service): https://github.com/esig/dss
