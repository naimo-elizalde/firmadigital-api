# Firma Digital API - Documentacion de Endpoints

**Base URL:** `https://{host}/api`
**Autenticacion:** JWT Bearer Token (`Authorization: Bearer <token>`)
**Content-Type:** `application/x-www-form-urlencoded`

---

## 1. Autenticacion

### 1.1 Obtener Token JWT

```
POST /auth/token
```

| Parametro | Tipo | Requerido | Descripcion |
|-----------|------|-----------|-------------|
| `subject` | String | Si | Identificador del usuario/sistema |
| `expirationMinutes` | Integer | No | Minutos de vigencia (1-1440) |

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expirationDate": "2026-05-15T12:00:00.000-05:00"
}
```

### 1.2 Validar Token

```
POST /auth/validate
```

| Parametro | Tipo | Requerido |
|-----------|------|-----------|
| `token` | String | Si |

**Response:**
```json
{
  "valid": true,
  "subject": "sistema-externo"
}
```

---

## 2. Firmar Documento

Firma un PDF con certificado PKCS#12. Sellado de tiempo (TSA) automatico.

```
POST /appfirmardocumento
```

**Requiere:** JWT

| Parametro | Tipo | Requerido | Descripcion |
|-----------|------|-----------|-------------|
| `pkcs12` | String | Si | Certificado .p12 en Base64 |
| `password` | String | Si | Clave del certificado |
| `documento` | String | Si | PDF en Base64 |
| `json` | String | No | Metadatos (ver abajo) |

**Metadatos opcionales:**
```json
{
  "razon": "Aprobacion del contrato",
  "localizacion": "Quito, Ecuador"
}
```

**Response exitosa:**
```json
{
  "resultado": "OK",
  "mensaje": "Documento firmado exitosamente",
  "documentoFirmado": "<PDF firmado en Base64>"
}
```

**Errores:**
```json
{ "resultado": "ERROR", "mensaje": "La clave del certificado es incorrecta" }
{ "resultado": "ERROR", "mensaje": "La clave del certificado es requerida" }
{ "resultado": "ERROR", "mensaje": "El archivo del certificado esta danado o no es un PKCS#12 valido" }
{ "resultado": "ERROR", "mensaje": "Error de autenticacion TSA (401): Credenciales incorrectas." }
{ "resultado": "ERROR", "mensaje": "Error de conflicto TSA (409): Hash duplicado." }
{ "resultado": "ERROR", "mensaje": "Error interno al firmar documento. Intente nuevamente" }
```

---

## 3. Firmar Documento con QR

Firma PDF con estampado visual QR + sellado de tiempo automatico.

```
POST /appfirmardocumentoconqr
```

**Requiere:** JWT

| Parametro | Tipo | Requerido | Descripcion |
|-----------|------|-----------|-------------|
| `pkcs12` | String | Si | Certificado .p12 en Base64 |
| `password` | String | Si | Clave del certificado |
| `documento` | String | Si | PDF en Base64 |
| `json` | String | No | Metadatos + config QR |

**Metadatos:**
```json
{
  "razon": "Aprobacion del contrato",
  "localizacion": "Quito, Ecuador",
  "infoQR": "https://verificar.ejemplo.com/doc/123",
  "qrPagina": 1,
  "qrPosX": 50,
  "qrPosY": 50,
  "qrAncho": 200,
  "qrAlto": 100
}
```

| Campo | Tipo | Default | Descripcion |
|-------|------|---------|-------------|
| `razon` | String | - | Razon de firma. Si no se envia, QR muestra "Firmado digitalmente con Nexus Soluciones" |
| `localizacion` | String | - | Ubicacion. Si no se envia, no aparece en QR ni metadatos |
| `infoQR` | String | - | Texto/URL adicional en QR |
| `qrPagina` | Integer | ultima | Pagina del QR |
| `qrPosX` | Float | 50 | X inferior izquierda (puntos PDF) |
| `qrPosY` | Float | 50 | Y inferior izquierda |
| `qrAncho` | Float | 200 | X superior derecha |
| `qrAlto` | Float | 100 | Y superior derecha |

**Estampado QR muestra:**
1. "Validar unicamente con Nexus Soluciones."
2. "Firmado electronicamente por:"
3. **NOMBRE DEL FIRMANTE** (negrita)

**Response exitosa:**
```json
{
  "resultado": "OK",
  "mensaje": "Documento firmado con QR exitosamente",
  "documentoFirmado": "<PDF firmado en Base64>"
}
```

**Errores:** Mismos que endpoint de firma simple.

---

## 4. Verificar Documento

Verifica firmas de un PDF: integridad, certificados, sellado de tiempo, datos del firmante.

```
POST /appverificardocumento
```

**Requiere:** JWT

| Parametro | Tipo | Requerido |
|-----------|------|-----------|
| `documento` | String | Si | PDF firmado en Base64 |

### Response - Con sellado de tiempo:

```json
{
  "resultado": "OK",
  "firmasValidas": true,
  "documentoIntegro": true,
  "numeroFirmas": 1,
  "firmas": [
    {
      "numeroFirma": 1,
      "nombreCompleto": "CARLOS ANDRES MARTINEZ LOPEZ",
      "entidadCertificadora": "LAZZATE CIA. LTDA",
      "fechaEmision": "2025-08-18T14:38:07.000-05:00",
      "fechaExpiracion": "2026-08-18T14:38:07.000-05:00",
      "fechaFirmado": "2026-05-15T11:02:09.000-05:00",
      "revocado": false,
      "certificadoVigente": true,
      "integridadFirma": true,
      "razon": "Aprobacion del contrato",
      "localizacion": "Quito, Ecuador",
      "usosLlave": "Firma Electronica, No Repudio, Cifrado de llave, ",
      "tieneSelladoTiempo": true,
      "selladoTiempo": {
        "fecha": "2026-05-15T11:02:09.000-05:00",
        "emitidoPor": "UANATACA S.A.",
        "valido": true,
        "autoridad": "Sello de tiempo acreditado de MINTEL - TSU02"
      },
      "datosUsuario": {
        "cedula": "0901234567",
        "nombreCompleto": "CARLOS ANDRES MARTINEZ LOPEZ",
        "tipoCertificado": "Persona Natural (EXT)",
        "certificadoDigitalValido": true
      }
    }
  ]
}
```

### Response - Sin sellado de tiempo:

```json
{
  "resultado": "OK",
  "firmasValidas": true,
  "documentoIntegro": true,
  "numeroFirmas": 1,
  "firmas": [
    {
      "numeroFirma": 1,
      "nombreCompleto": "MARIA FERNANDA TORRES RIVAS",
      "entidadCertificadora": "SECURITY DATA",
      "fechaEmision": "2025-01-15T10:00:00.000-05:00",
      "fechaExpiracion": "2027-01-15T10:00:00.000-05:00",
      "fechaFirmado": "2026-05-15T09:30:00.000-05:00",
      "revocado": false,
      "certificadoVigente": true,
      "integridadFirma": true,
      "razon": null,
      "localizacion": null,
      "usosLlave": "Firma Electronica, No Repudio, ",
      "tieneSelladoTiempo": false,
      "datosUsuario": {
        "cedula": "1710987654",
        "nombreCompleto": "MARIA FERNANDA TORRES RIVAS",
        "tipoCertificado": "",
        "certificadoDigitalValido": true
      }
    }
  ]
}
```

### Response - Sin firmas:

```json
{
  "resultado": "OK",
  "firmasValidas": false,
  "documentoIntegro": false,
  "numeroFirmas": 0,
  "mensaje": "El documento no contiene firmas digitales"
}
```

### Response - Certificado revocado:

```json
{
  "resultado": "OK",
  "firmasValidas": false,
  "documentoIntegro": true,
  "numeroFirmas": 1,
  "firmas": [
    {
      "numeroFirma": 1,
      "nombreCompleto": "MARIA FERNANDA TORRES RIVAS",
      "entidadCertificadora": "LAZZATE CIA. LTDA",
      "fechaFirmado": "2026-05-15T11:00:00.000-05:00",
      "fechaRevocacion": "2026-04-01T00:00:00.000-05:00",
      "revocado": true,
      "certificadoVigente": false,
      "integridadFirma": true,
      "tieneSelladoTiempo": false,
      "datosUsuario": {
        "cedula": "1710987654",
        "nombreCompleto": "MARIA FERNANDA TORRES RIVAS",
        "certificadoDigitalValido": false
      }
    }
  ]
}
```

### Campos de respuesta verificacion:

**Nivel documento:**

| Campo | Tipo | Descripcion |
|-------|------|-------------|
| `firmasValidas` | Boolean | Todas las firmas validas |
| `documentoIntegro` | Boolean | Documento no modificado post-firma |
| `numeroFirmas` | Integer | Cantidad de firmas |

**Por firma:**

| Campo | Tipo | Descripcion |
|-------|------|-------------|
| `nombreCompleto` | String | Nombre del firmante (CN) |
| `entidadCertificadora` | String | CA emisora del certificado |
| `fechaEmision` | String | Inicio vigencia certificado |
| `fechaExpiracion` | String | Fin vigencia certificado |
| `fechaFirmado` | String | Cuando se firmo el documento |
| `revocado` | Boolean | Si el certificado esta revocado |
| `fechaRevocacion` | String | Fecha revocacion (solo si revocado) |
| `certificadoVigente` | Boolean | Certificado vigente al firmar |
| `integridadFirma` | Boolean | Firma criptograficamente valida |
| `razon` | String | Razon de firma |
| `localizacion` | String | Ubicacion de firma |
| `usosLlave` | String | Key usages del certificado |
| `tieneSelladoTiempo` | Boolean | Si tiene sello TSA |

**Sellado de tiempo (`selladoTiempo`):**

| Campo | Tipo | Descripcion |
|-------|------|-------------|
| `fecha` | String | Fecha/hora del sello TSA |
| `emitidoPor` | String | CA del certificado TSA |
| `valido` | Boolean | Sello verificado contra CA raiz |
| `autoridad` | String | CN del certificado TSA |

**Datos usuario (`datosUsuario`):**

| Campo | Tipo | Descripcion |
|-------|------|-------------|
| `cedula` | String | Cedula de identidad |
| `nombreCompleto` | String | Nombre completo del firmante |
| `tipoCertificado` | String | Tipo (Persona Natural (EXT), Persona Juridica (EXT), etc) |
| `certificadoDigitalValido` | Boolean | CA ecuatoriana reconocida |

---

## 5. Validar Certificado Digital

Valida un certificado .p12 sin firmar: vigencia, revocacion, datos del titular.

```
POST /appvalidarcertificadodigital
```

**Requiere:** JWT

| Parametro | Tipo | Requerido |
|-----------|------|-----------|
| `pkcs12` | String | Si | Certificado .p12 en Base64 |
| `password` | String | Si | Clave |

### Response exitosa:

```json
{
  "resultado": "OK",
  "vigente": true,
  "estado": "VIGENTE",
  "serial": "123456789",
  "entidadCertificadora": "LAZZATE CIA. LTDA",
  "fechaEmision": "2025-08-18T14:38:07.000-05:00",
  "fechaExpiracion": "2026-08-18T14:38:07.000-05:00",
  "algoritmo": "SHA256withRSA",
  "diasHastaExpiracion": 460,
  "revocacion": {
    "consultada": true,
    "revocado": false,
    "detalle": "No revocado"
  },
  "datosUsuario": {
    "cedula": "0901234567",
    "nombre": "CARLOS ANDRES",
    "apellido": "MARTINEZ LOPEZ",
    "institucion": "",
    "cargo": "",
    "tipoCertificado": "Persona Natural (EXT)",
    "certificadoDigitalValido": true
  }
}
```

### Response - Certificado expirado:

```json
{
  "resultado": "OK",
  "vigente": false,
  "estado": "EXPIRADO",
  "motivoNoValido": "El certificado ha expirado",
  "diasHastaExpiracion": -30,
  "revocacion": {
    "consultada": true,
    "revocado": false,
    "detalle": "No revocado"
  },
  "datosUsuario": { ... }
}
```

### Response - Certificado revocado:

```json
{
  "resultado": "OK",
  "vigente": false,
  "estado": "REVOCADO",
  "motivoNoValido": "El certificado esta revocado",
  "revocacion": {
    "consultada": true,
    "revocado": true,
    "fecha": "2026-04-01T00:00:00.000-05:00",
    "detalle": "KEY_COMPROMISE"
  },
  "datosUsuario": { ... }
}
```

**Estados posibles:** `VIGENTE`, `EXPIRADO`, `NO_VIGENTE_AUN`, `REVOCADO`

**Errores:**
```json
{ "resultado": "ERROR", "mensaje": "La clave del certificado es incorrecta", "vigente": false }
{ "resultado": "ERROR", "mensaje": "El archivo del certificado esta danado o no es un PKCS#12 valido", "vigente": false }
{ "resultado": "ERROR", "mensaje": "Error interno al validar certificado. Intente nuevamente", "vigente": false }
```

---

## 6. Consultar Certificado (Revocacion)

### 6.1 Verificar si esta revocado

```
GET /certificado/revocado/{serial}
```

**Response:** `true` o `false` (text/plain)

### 6.2 Fecha de revocacion

```
GET /certificado/fechaRevocado/{serial}
```

**Response:** Fecha o vacio (text/plain)

---

## 7. Fecha y Hora del Servidor

### GET (JSON)

```
GET /fecha-hora
```

```json
{
  "fecha": "2026-05-15T11:02:09.000-05:00",
  "timestamp": 1778896929000,
  "timezone": "America/Guayaquil"
}
```

### POST (text/plain)

```
POST /fecha-hora
```

**Response:** `2026-05-15T11:02:09-05:00`

---

## 8. Version

```
GET /version
```

```json
{
  "version": "4.1.0"
}
```

---

## 9. Errores

| HTTP | Causa |
|------|-------|
| `401` | Token JWT invalido/expirado/ausente |
| `413` | Request excede tamano maximo |
| `200` + `ERROR` | Error de negocio |

**Estructura error JWT (401):**
```json
{
  "resultado": "ERROR",
  "mensaje": "Token JWT no proporcionado",
  "codigo": 401
}
```

**Estructura error negocio (200):**
```json
{
  "resultado": "ERROR",
  "mensaje": "Descripcion del error"
}
```

**Errores comunes:**

| Mensaje | Causa |
|---------|-------|
| `La clave del certificado es incorrecta` | Clave erronea |
| `La clave del certificado es requerida` | Parametro `password` vacio |
| `El archivo del certificado esta danado o no es un PKCS#12 valido` | .p12 corrupto o formato invalido |
| `Error de autenticacion TSA (401): Credenciales incorrectas.` | Cedula rechazada por servidor TSA |
| `Error de conflicto TSA (409): Hash duplicado.` | Documento ya sellado |
| `El documento no esta correctamente codificado en Base64` | Base64 invalido |
| `Error interno al firmar documento. Intente nuevamente` | Error inesperado |

---

## 10. Sellado de Tiempo (TSA)

- **Automatico:** Cedula extraida del .p12 via `CertEcUtils.getDatosUsuarios()`
- **Servidor:** `https://tsa.uanatacaec.com/tsa/timestamp` (UANATACA S.A.)
- **Protocolo:** RFC 3161
- **Sin cedula extraible:** Firma sin sello de tiempo (PAdES basico)

### CAs ecuatorianas soportadas

BCE, Security Data, ANFAC, Consejo de la Judicatura, UANATACA, DIGERCIC, Eclipsoft, Datil, ArgosData, Lazzate, Alpha Technologies, CorpNewBest, FirmaSegura, Letmi, AppFirmas
