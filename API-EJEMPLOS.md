# API de Firma Digital - Guía de Uso y Ejemplos

## 📋 Índice

1. [Información General](#información-general)
2. [Configuración Base](#configuración-base)
3. [Endpoints Disponibles](#endpoints-disponibles)
4. [Ejemplos de Peticiones](#ejemplos-de-peticiones)
5. [Respuestas y Códigos de Estado](#respuestas-y-códigos-de-estado)
6. [Ejemplos con cURL](#ejemplos-con-curl)
7. [Ejemplos con JavaScript/Fetch](#ejemplos-con-javascriptfetch)
8. [Ejemplos con Postman](#ejemplos-con-postman)

---

## 📌 Información General

**URL Base de la API**: `http://localhost:8080/api`

**Versión**: 4.1.0

**⚠️ IMPORTANTE - Arquitectura Standalone**: Esta API ha sido modificada para funcionar de manera **completamente independiente** sin requerir servicios externos. Los endpoints de validación de versión y fecha/hora funcionan directamente en el servidor sin hacer llamadas a servicios de terceros.

La API de Firma Digital proporciona servicios para:
- Firmar documentos digitalmente (PDF, XML)
- Validar certificados digitales
- Verificar documentos firmados
- Obtener información de certificados
- Obtener fecha y hora del servidor (sin dependencias externas)

---

## ⚙️ Configuración Base

### Headers Comunes

```http
Content-Type: application/x-www-form-urlencoded
Accept: application/json
```

---

## 🔗 Endpoints Disponibles

### 1. Validar Versión
- **Endpoint**: `/version`
- **Método**: `POST`
- **Descripción**: Valida la versión de la aplicación cliente

### 2. Obtener Fecha y Hora del Servidor
- **Endpoint**: `/fecha-hora`
- **Método**: `POST`
- **Descripción**: Obtiene la fecha y hora del servidor en formato ISO-8601

### 3. Firmar Documento
- **Endpoint**: `/appfirmardocumento`
- **Método**: `POST`
- **Descripción**: Firma un documento digital con un certificado PKCS#12

### 4. Validar Certificado Digital
- **Endpoint**: `/appvalidarcertificadodigital`
- **Método**: `POST`
- **Descripción**: Valida un certificado digital PKCS#12

### 5. Verificar Documento Firmado
- **Endpoint**: `/appverificardocumento`
- **Método**: `POST`
- **Descripción**: Verifica la firma digital de un documento

### 6. Verificar Estado de Certificado
- **Endpoint**: `/certificado/revocado/{serial}`
- **Método**: `GET`
- **Descripción**: Verifica si un certificado está revocado

### 7. Fecha de Revocación de Certificado
- **Endpoint**: `/certificado/fechaRevocado/{serial}`
- **Método**: `GET`
- **Descripción**: Obtiene la fecha de revocación de un certificado

---

## 📝 Ejemplos de Peticiones

### 1. Validar Versión

**Endpoint**: `POST /api/version`

**Parámetros**:
- `base64` (string): Token de versión en Base64

**Ejemplo de Petición**:
```http
POST http://localhost:8080/api/version
Content-Type: application/x-www-form-urlencoded

base64=NDQuMS4w
```

**Respuesta Exitosa**:
```json
{
  "resultado": "Version enabled",
  "versionServidor": "4.1.0",
  "versionCliente": "4.1.0",
  "compatible": true
}
```

**Nota**: También puedes usar `GET /api/version` para obtener solo la versión del servidor:
```bash
curl http://localhost:8080/api/version
```

Respuesta:
```json
{
  "version": "4.1.0",
  "resultado": "OK"
}
```

---

### 2. Obtener Fecha y Hora del Servidor

**Endpoint**: `POST /api/fecha-hora`

**Parámetros**:
- `base64` (string): Token de autorización en Base64

**Ejemplo de Petición**:
```http
POST http://localhost:8080/api/fecha-hora
Content-Type: application/x-www-form-urlencoded

base64=NDQuMS4w
```

**Respuesta Exitosa**:
```text
2025-11-11T10:30:45.123-05:00
```

**Nota**: También puedes usar `GET /api/fecha-hora` para obtener información detallada en JSON:
```bash
curl http://localhost:8080/api/fecha-hora
```

Respuesta:
```json
{
  "fechaHora": "2025-11-11T10:30:45.123-05:00",
  "timestamp": 1731347445,
  "zonaHoraria": "America/Guayaquil",
  "resultado": "OK"
}
```

---

### 3. Firmar Documento

**Endpoint**: `POST /api/appfirmardocumento`

**Parámetros**:
- `jwt` (string): Token JWT de autenticación
- `pkcs12` (string): Certificado digital en formato PKCS#12 codificado en Base64
- `password` (string): Contraseña del certificado PKCS#12
- `documento` (string): Documento a firmar codificado en Base64
- `json` (string): Metadatos de la firma en formato JSON (opcional)
- `base64` (string): Token de versión en Base64

**Ejemplo de Petición**:
```http
POST http://localhost:8080/api/appfirmardocumento
Content-Type: application/x-www-form-urlencoded

jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
&pkcs12=MIIJ...BASE64_ENCODED_CERTIFICATE...==
&password=MiContraseñaSegura123
&documento=JVBERi0...BASE64_ENCODED_PDF...==
&json={"razon":"Firma de aprobación","localizacion":"Quito, Ecuador","cargo":"Director"}
&base64=NDQuMS4w
```

**Respuesta Exitosa**:
```json
{
  "resultado": "OK",
  "mensaje": "Documento firmado exitosamente",
  "documentoFirmado": "JVBERi0...BASE64_ENCODED_SIGNED_PDF...=="
}
```

---

### 4. Validar Certificado Digital

**Endpoint**: `POST /api/appvalidarcertificadodigital`

**Parámetros**:
- `jwt` (string): Token JWT de autenticación
- `pkcs12` (string): Certificado digital en formato PKCS#12 codificado en Base64
- `password` (string): Contraseña del certificado PKCS#12
- `base64` (string): Token de versión en Base64

**Ejemplo de Petición**:
```http
POST http://localhost:8080/api/appvalidarcertificadodigital
Content-Type: application/x-www-form-urlencoded

jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
&pkcs12=MIIJ...BASE64_ENCODED_CERTIFICATE...==
&password=MiContraseñaSegura123
&base64=NDQuMS4w
```

**Respuesta Exitosa**:
```json
{
  "resultado": "OK",
  "valido": true,
  "subject": "CN=Juan Pérez,OU=CIUDADANO,O=BCE,C=EC",
  "issuer": "CN=AC BANCO CENTRAL DEL ECUADOR,OU=ENTIDAD DE CERTIFICACION,O=BCE,C=EC",
  "serialNumber": "123456789",
  "notBefore": "2023-01-01T00:00:00Z",
  "notAfter": "2026-01-01T23:59:59Z",
  "estado": "VIGENTE"
}
```

---

### 5. Verificar Documento Firmado

**Endpoint**: `POST /api/appverificardocumento`

**Parámetros**:
- `jwt` (string): Token JWT de autenticación
- `documento` (string): Documento firmado codificado en Base64
- `base64` (string): Token de versión en Base64

**Ejemplo de Petición**:
```http
POST http://localhost:8080/api/appverificardocumento
Content-Type: application/x-www-form-urlencoded

jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
&documento=JVBERi0...BASE64_ENCODED_SIGNED_PDF...==
&base64=NDQuMS4w
```

**Respuesta Exitosa**:
```json
{
  "resultado": "OK",
  "firmaValida": true,
  "firmas": [
    {
      "firmante": "Juan Pérez",
      "fechaFirma": "2025-11-11T10:30:45-05:00",
      "razon": "Firma de aprobación",
      "localizacion": "Quito, Ecuador",
      "certificadoValido": true
    }
  ]
}
```

---

### 6. Verificar Estado de Certificado

**Endpoint**: `GET /api/certificado/revocado/{serial}`

**Parámetros de URL**:
- `serial` (número): Número de serie del certificado

**Ejemplo de Petición**:
```http
GET http://localhost:8080/api/certificado/revocado/123456789
Accept: text/plain
```

**Respuesta Exitosa**:
```text
false
```

---

### 7. Obtener Fecha de Revocación

**Endpoint**: `GET /api/certificado/fechaRevocado/{serial}`

**Parámetros de URL**:
- `serial` (número): Número de serie del certificado

**Ejemplo de Petición**:
```http
GET http://localhost:8080/api/certificado/fechaRevocado/123456789
Accept: text/plain
```

**Respuesta Exitosa**:
```text
2024-06-15T14:30:00Z
```

O si no está revocado:
```text
null
```

---

## 🔨 Ejemplos con cURL

### Validar Versión
```bash
curl -X POST http://localhost:8080/api/version \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "base64=NDQuMS4w"
```

### Obtener Fecha y Hora
```bash
curl -X POST http://localhost:8080/api/fecha-hora \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "base64=NDQuMS4w"
```

### Firmar Documento
```bash
curl -X POST http://localhost:8080/api/appfirmardocumento \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d "pkcs12=$(cat certificado.p12 | base64 -w 0)" \
  -d "password=MiContraseña123" \
  -d "documento=$(cat documento.pdf | base64 -w 0)" \
  -d "json={\"razon\":\"Firma de aprobación\",\"localizacion\":\"Quito\"}" \
  -d "base64=NDQuMS4w"
```

### Validar Certificado
```bash
curl -X POST http://localhost:8080/api/appvalidarcertificadodigital \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d "pkcs12=$(cat certificado.p12 | base64 -w 0)" \
  -d "password=MiContraseña123" \
  -d "base64=NDQuMS4w"
```

### Verificar Documento
```bash
curl -X POST http://localhost:8080/api/appverificardocumento \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d "documento=$(cat documento_firmado.pdf | base64 -w 0)" \
  -d "base64=NDQuMS4w"
```

### Verificar Estado de Certificado
```bash
curl -X GET http://localhost:8080/api/certificado/revocado/123456789 \
  -H "Accept: text/plain"
```

---

## 💻 Ejemplos con JavaScript/Fetch

### Validar Versión
```javascript
async function validarVersion() {
  const response = await fetch('http://localhost:8080/api/version', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams({
      base64: 'NDQuMS4w'
    })
  });
  
  const data = await response.text();
  console.log(data);
  return data;
}
```

### Obtener Fecha y Hora
```javascript
async function obtenerFechaHora() {
  const response = await fetch('http://localhost:8080/api/fecha-hora', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams({
      base64: 'NDQuMS4w'
    })
  });
  
  const fechaHora = await response.text();
  console.log('Fecha y hora del servidor:', fechaHora);
  return fechaHora;
}
```

### Firmar Documento
```javascript
async function firmarDocumento(jwt, certificadoP12Base64, password, documentoBase64, metadata) {
  const response = await fetch('http://localhost:8080/api/appfirmardocumento', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams({
      jwt: jwt,
      pkcs12: certificadoP12Base64,
      password: password,
      documento: documentoBase64,
      json: JSON.stringify(metadata),
      base64: 'NDQuMS4w'
    })
  });
  
  const resultado = await response.json();
  console.log('Resultado de la firma:', resultado);
  return resultado;
}

// Uso:
const metadata = {
  razon: "Firma de aprobación",
  localizacion: "Quito, Ecuador",
  cargo: "Director"
};

firmarDocumento(
  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...',
  certificadoBase64,
  'MiContraseña123',
  documentoBase64,
  metadata
);
```

### Validar Certificado
```javascript
async function validarCertificado(jwt, certificadoP12Base64, password) {
  const response = await fetch('http://localhost:8080/api/appvalidarcertificadodigital', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams({
      jwt: jwt,
      pkcs12: certificadoP12Base64,
      password: password,
      base64: 'NDQuMS4w'
    })
  });
  
  const resultado = await response.json();
  console.log('Validación del certificado:', resultado);
  return resultado;
}
```

### Verificar Documento Firmado
```javascript
async function verificarDocumento(jwt, documentoFirmadoBase64) {
  const response = await fetch('http://localhost:8080/api/appverificardocumento', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams({
      jwt: jwt,
      documento: documentoFirmadoBase64,
      base64: 'NDQuMS4w'
    })
  });
  
  const resultado = await response.json();
  console.log('Verificación del documento:', resultado);
  return resultado;
}
```

### Leer archivo y convertir a Base64
```javascript
function archivoABase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const base64 = reader.result.split(',')[1];
      resolve(base64);
    };
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

// Uso con input file
document.getElementById('fileInput').addEventListener('change', async (e) => {
  const file = e.target.files[0];
  const base64 = await archivoABase64(file);
  console.log('Archivo en Base64:', base64);
});
```

---

## 📮 Ejemplos con Postman

### Configuración de Colección

#### 1. Crear Variables de Entorno
```
BASE_URL: http://localhost:8080/api
JWT_TOKEN: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
VERSION_TOKEN: NDQuMS4w
```

#### 2. Request: Validar Versión
```
Method: POST
URL: {{BASE_URL}}/version
Headers:
  Content-Type: application/x-www-form-urlencoded
Body (x-www-form-urlencoded):
  base64: {{VERSION_TOKEN}}
```

#### 3. Request: Firmar Documento
```
Method: POST
URL: {{BASE_URL}}/appfirmardocumento
Headers:
  Content-Type: application/x-www-form-urlencoded
Body (x-www-form-urlencoded):
  jwt: {{JWT_TOKEN}}
  pkcs12: <CERTIFICADO_BASE64>
  password: MiContraseña123
  documento: <DOCUMENTO_BASE64>
  json: {"razon":"Firma de aprobación","localizacion":"Quito"}
  base64: {{VERSION_TOKEN}}
```

#### 4. Pre-request Script para Base64 (Postman)
```javascript
// Leer un archivo local y convertirlo a Base64
const fs = require('fs');
const path = require('path');

// Leer certificado
const certPath = path.join(__dirname, 'certificado.p12');
const certBuffer = fs.readFileSync(certPath);
const certBase64 = certBuffer.toString('base64');
pm.environment.set("CERT_BASE64", certBase64);

// Leer documento
const docPath = path.join(__dirname, 'documento.pdf');
const docBuffer = fs.readFileSync(docPath);
const docBase64 = docBuffer.toString('base64');
pm.environment.set("DOC_BASE64", docBase64);
```

---

## 📊 Respuestas y Códigos de Estado

### Códigos HTTP
- `200 OK`: Petición exitosa
- `406 Not Acceptable`: Error en la validación
- `500 Internal Server Error`: Error interno del servidor
- `404 Not Found`: Recurso no encontrado

### Formato de Respuestas de Error
```json
{
  "error": "Descripción del error",
  "mensaje": "Detalle adicional del error"
}
```

### Ejemplos de Errores Comunes

#### Certificado Inválido
```json
{
  "resultado": "ERROR",
  "mensaje": "Certificado digital inválido o contraseña incorrecta"
}
```

#### Token JWT Expirado
```json
{
  "resultado": "ERROR",
  "mensaje": "Token JWT expirado o inválido"
}
```

#### Documento No Firmado
```json
{
  "resultado": "ERROR",
  "mensaje": "El documento no contiene firmas digitales"
}
```

---

## 🔐 Consideraciones de Seguridad

1. **JWT Token**: Siempre usa tokens JWT válidos y renovados
2. **HTTPS**: En producción, usa HTTPS para todas las comunicaciones
3. **Contraseñas**: Nunca guardes contraseñas de certificados en código
4. **Base64**: Los archivos en Base64 pueden ser grandes, considera límites de tamaño
5. **Timeouts**: Configura timeouts adecuados para operaciones largas

---

## 🧪 Testing

### Script de Prueba Completo (Node.js)
```javascript
const axios = require('axios');
const fs = require('fs');

const BASE_URL = 'http://localhost:8080/api';

async function testAPI() {
  try {
    // 1. Validar versión
    console.log('1. Validando versión...');
    const versionRes = await axios.post(`${BASE_URL}/version`, 
      new URLSearchParams({ base64: 'NDQuMS4w' }),
      { headers: { 'Content-Type': 'application/x-www-form-urlencoded' }}
    );
    console.log('✓ Versión:', versionRes.data);

    // 2. Obtener fecha y hora
    console.log('\n2. Obteniendo fecha y hora...');
    const fechaRes = await axios.post(`${BASE_URL}/fecha-hora`,
      new URLSearchParams({ base64: 'NDQuMS4w' }),
      { headers: { 'Content-Type': 'application/x-www-form-urlencoded' }}
    );
    console.log('✓ Fecha y hora:', fechaRes.data);

    // 3. Verificar estado de certificado
    console.log('\n3. Verificando estado de certificado...');
    const certRes = await axios.get(`${BASE_URL}/certificado/revocado/123456789`);
    console.log('✓ Certificado revocado:', certRes.data);

    console.log('\n✅ Todas las pruebas pasaron correctamente');
  } catch (error) {
    console.error('❌ Error en las pruebas:', error.message);
    if (error.response) {
      console.error('Respuesta del servidor:', error.response.data);
    }
  }
}

testAPI();
```

---

## 📚 Recursos Adicionales

- **Documentación de Jakarta EE**: https://jakarta.ee/
- **Especificación JAX-RS**: https://jakarta.ee/specifications/restful-ws/
- **Formato Base64**: https://developer.mozilla.org/en-US/docs/Glossary/Base64
- **JWT (JSON Web Tokens)**: https://jwt.io/

---

## 🆘 Soporte y Contacto

Para reportar problemas o solicitar soporte:
- **Email**: soporte@firmadigital.gob.ec
- **Repositorio**: https://minka.gob.ec/snap/firmadigital-api

---

**Versión de la documentación**: 1.0.0  
**Última actualización**: 11 de noviembre de 2025
