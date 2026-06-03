# Firma Digital API - Documentación de Endpoints

**Base URL:** `http://localhost:8080`

Todos los endpoints (excepto `/auth/*`) requieren autenticación JWT via header `Authorization: Bearer <token>`.

---

## 1. Autenticación

### POST `/auth/token`

Genera un token JWT para autenticación.

**Content-Type:** `application/x-www-form-urlencoded`

| Parámetro | Tipo | Requerido | Descripción |
|---|---|---|---|
| `subject` | string | Sí | Identificador del usuario/aplicación |
| `expirationMinutes` | string | No | Minutos hasta expiración (1-1440, default: 60) |

**Respuesta exitosa:**
```json
{
  "resultado": "OK",
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "subject": "mi-aplicacion",
  "expirationMinutes": 60,
  "tipo": "Bearer"
}
```

**Errores:**
| Caso | Mensaje |
|---|---|
| `subject` vacío o null | `"El parámetro 'subject' es requerido"` |
| `expirationMinutes` no numérico | `"El parámetro 'expirationMinutes' debe ser un número válido"` |
| `expirationMinutes` fuera de rango | `"El tiempo de expiración debe estar entre 1 y 1440 minutos"` |

---

### POST `/auth/validate`

Valida un token JWT y retorna su información.

**Content-Type:** `application/x-www-form-urlencoded`

| Parámetro | Tipo | Requerido | Descripción |
|---|---|---|---|
| `token` | string | Sí | Token JWT a validar |

**Respuesta exitosa (token válido):**
```json
{
  "resultado": "OK",
  "valido": true,
  "subject": "mi-aplicacion",
  "expiracion": "Mon Jun 02 20:00:00 UTC 2026"
}
```

**Respuesta (token inválido/expirado):**
```json
{
  "resultado": "OK",
  "valido": false,
  "mensaje": "Token inválido o expirado"
}
```

---

## 2. Firmar Documento con QR

### POST `/appfirmardocumentoconqr`

Firma un documento PDF con firma digital y código QR visible.

**Requiere:** JWT (header `Authorization: Bearer <token>`)

**Content-Type:** `application/x-www-form-urlencoded`

| Parámetro | Tipo | Requerido | Descripción |
|---|---|---|---|
| `pkcs12` | string | Sí | Certificado PKCS#12 (.p12) en Base64 |
| `password` | string | Sí | Contraseña del certificado |
| `documento` | string | Sí | Documento PDF en Base64 |
| `json` | string | No | Metadatos JSON de la firma (ver abajo) |

**Estructura del parámetro `json`:**
```json
{
  "razon": "Aprobación de documento",
  "localizacion": "Quito, Ecuador",
  "cargo": "Gerente General",
  "identificacion": "1234567890",
  "infoQR": "Texto adicional para el QR",
  "qrPagina": 1,
  "qrPosX": 50,
  "qrPosY": 50,
  "qrAncho": 200,
  "qrAlto": 100
}
```

| Campo JSON | Tipo | Default | Descripción |
|---|---|---|---|
| `razon` | string | `"Firma digital"` | Razón de la firma |
| `localizacion` | string | - | Ubicación del firmante |
| `cargo` | string | - | Cargo del firmante |
| `identificacion` | string | (se extrae del certificado) | Cédula/identificación |
| `infoQR` | string | - | Texto adicional en el QR |
| `qrPagina` | int | - | Página donde colocar el QR |
| `qrPosX` | float | 50 | Posición X (esquina inferior izquierda) |
| `qrPosY` | float | 50 | Posición Y (esquina inferior izquierda) |
| `qrAncho` | float | 200 (mín: 150) | Ancho del recuadro de firma |
| `qrAlto` | float | 100 (mín: 55) | Alto del recuadro de firma |

**Respuesta exitosa:**
```json
{
  "resultado": "OK",
  "mensaje": "Documento firmado con QR exitosamente",
  "documentoFirmado": "JVBERi0xLjQK..."
}
```

**Errores:**
| Caso | Mensaje |
|---|---|
| Sin certificado | `"El certificado PKCS#12 es requerido"` |
| Sin contraseña | `"La clave del certificado es requerida"` |
| Sin documento | `"El documento a firmar es requerido"` |
| Contraseña incorrecta | `"La clave del certificado es incorrecta"` |
| Certificado corrupto | `"El archivo del certificado esta danado o no es un PKCS#12 valido"` |
| Llave privada no encontrada | `"No se pudo obtener la llave privada del certificado"` |
| Cadena de certificados vacía | `"No se pudo obtener la cadena de certificados"` |
| Error de formato Base64 | `"Error de formato: ..."` |
| Error TSA | Mensaje específico del TSA |
| Error interno | `"Error interno al firmar documento. Intente nuevamente"` |

---

## 3. Validar Certificado Digital

### POST `/appvalidarcertificadodigital`

Valida un certificado digital PKCS#12: vigencia, revocación y datos del titular.

**Requiere:** JWT (header `Authorization: Bearer <token>`)

**Content-Type:** `application/x-www-form-urlencoded`

| Parámetro | Tipo | Requerido | Descripción |
|---|---|---|---|
| `pkcs12` | string | Sí | Certificado PKCS#12 (.p12) en Base64 |
| `password` | string | Sí | Contraseña del certificado |

**Respuesta exitosa:**
```json
{
  "resultado": "OK",
  "vigente": true,
  "estado": "VIGENTE",
  "serial": "123456789",
  "entidadCertificadora": "Security Data",
  "fechaEmision": "2024-01-15T10:30:00.000-05:00",
  "fechaExpiracion": "2026-01-15T10:30:00.000-05:00",
  "algoritmo": "SHA256withRSA",
  "subject": "CN=GONZALEZ JIMENEZ LUIS XAVIER, ...",
  "issuer": "CN=Security Data CA, ...",
  "diasHastaExpiracion": 227,
  "advertencia": "El certificado expirara en menos de 30 dias",
  "revocacion": {
    "consultada": true,
    "revocado": false,
    "detalle": "No revocado"
  },
  "datosUsuario": {
    "cedula": "1234567890",
    "nombre": "LUIS XAVIER",
    "apellido": "GONZALEZ JIMENEZ",
    "institucion": "EMPRESA S.A.",
    "cargo": "GERENTE",
    "tipoCertificado": "PERSONA_NATURAL",
    "certificadoDigitalValido": true
  }
}
```

**Estados posibles:**
| Estado | Descripción |
|---|---|
| `VIGENTE` | Certificado válido y no revocado |
| `EXPIRADO` | Certificado ha expirado |
| `NO_VIGENTE_AUN` | Certificado aún no es válido (fecha futura) |
| `REVOCADO` | Certificado fue revocado por la CA |

**Errores:**
| Caso | Mensaje |
|---|---|
| Sin certificado | `"El certificado PKCS#12 es requerido"` |
| Sin contraseña | `"La clave del certificado es requerida"` |
| Contraseña incorrecta | `"La clave del certificado es incorrecta"` |
| Certificado corrupto | `"El archivo del certificado esta danado o no es un PKCS#12 valido"` |
| Base64 inválido | `"El certificado no esta correctamente codificado en Base64"` |
| Error interno | `"Error interno al validar certificado. Intente nuevamente"` |

---

## 4. Verificar Documento Firmado

### POST `/appverificardocumento`

Verifica las firmas digitales de un documento PDF firmado. Retorna información de cada firma, integridad, sellado de tiempo y datos del firmante.

**Requiere:** JWT (header `Authorization: Bearer <token>`)

**Content-Type:** `application/x-www-form-urlencoded`

| Parámetro | Tipo | Requerido | Descripción |
|---|---|---|---|
| `documento` | string | Sí | Documento PDF firmado en Base64 |

**Respuesta exitosa (documento con firmas):**
```json
{
  "resultado": "OK",
  "firmasValidas": true,
  "documentoIntegro": true,
  "numeroFirmas": 2,
  "firmas": [
    {
      "numeroFirma": 1,
      "nombreCompleto": "GONZALEZ JIMENEZ LUIS XAVIER",
      "entidadCertificadora": "Security Data",
      "fechaEmision": "2024-01-15T10:30:00.000-05:00",
      "fechaExpiracion": "2026-01-15T10:30:00.000-05:00",
      "fechaFirmado": "2026-06-02T14:46:27.000-05:00",
      "revocado": false,
      "certificadoVigente": true,
      "integridadFirma": true,
      "razon": "Aprobación",
      "localizacion": "Quito",
      "usosLlave": "digitalSignature, nonRepudiation",
      "tieneSelladoTiempo": true,
      "selladoTiempo": {
        "fecha": "2026-06-02T14:46:28.000-05:00",
        "emitidoPor": "TSA Authority",
        "valido": true,
        "autoridad": "CN=TSA Authority"
      },
      "datosUsuario": {
        "cedula": "1234567890",
        "nombreCompleto": "LUIS XAVIER GONZALEZ JIMENEZ",
        "tipoCertificado": "PERSONA_NATURAL",
        "certificadoDigitalValido": true
      }
    }
  ]
}
```

**Respuesta (documento sin firmas):**
```json
{
  "resultado": "OK",
  "firmasValidas": false,
  "documentoIntegro": false,
  "numeroFirmas": 0,
  "mensaje": "El documento no contiene firmas digitales"
}
```

**Errores:**
| Caso | Mensaje |
|---|---|
| Sin documento | `"El documento es requerido"` |
| Base64 inválido | `"El documento no esta correctamente codificado en Base64"` |
| PDF inválido | `"El documento no es un PDF valido o no contiene firmas reconocibles"` |
| Error interno | `"Error interno al verificar documento. Intente nuevamente"` |

---

## Formato de error general

Todos los endpoints devuelven errores con esta estructura:

```json
{
  "resultado": "ERROR",
  "mensaje": "Descripción del error"
}
```

Algunos endpoints agregan campos adicionales al error:
- `/appvalidarcertificadodigital` agrega `"vigente": false`
- `/appverificardocumento` agrega `"firmasValidas": false`

---

## Ejemplo de uso completo (cURL)

### 1. Obtener token
```bash
curl -X POST http://localhost:8080/auth/token \
  -d "subject=mi-app" \
  -d "expirationMinutes=120"
```

### 2. Firmar documento con QR
```bash
curl -X POST http://localhost:8080/appfirmardocumentoconqr \
  -H "Authorization: Bearer eyJhbGciOiJI..." \
  -d "pkcs12=$(base64 -i certificado.p12)" \
  -d "password=mi-clave" \
  -d "documento=$(base64 -i documento.pdf)" \
  -d 'json={"razon":"Aprobado","localizacion":"Quito","qrPosX":50,"qrPosY":50,"qrAncho":200,"qrAlto":100}'
```

### 3. Validar certificado
```bash
curl -X POST http://localhost:8080/appvalidarcertificadodigital \
  -H "Authorization: Bearer eyJhbGciOiJI..." \
  -d "pkcs12=$(base64 -i certificado.p12)" \
  -d "password=mi-clave"
```

### 4. Verificar documento firmado
```bash
curl -X POST http://localhost:8080/appverificardocumento \
  -H "Authorization: Bearer eyJhbGciOiJI..." \
  -d "documento=$(base64 -i documento-firmado.pdf)"
```
