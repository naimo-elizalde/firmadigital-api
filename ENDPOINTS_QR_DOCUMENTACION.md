# Nuevos Endpoints - Estampado QR y Firma Digital

## Descripción General

Se han agregado dos nuevos endpoints a la API de Firma Digital para soportar el estampado visual con código QR en documentos PDF:

1. **`/appestampadoqr`** - Agrega solo estampado visual con QR (sin firma digital)
2. **`/appfirmardocumentoconqr`** - Agrega estampado QR y luego firma digitalmente el documento

## 1. Endpoint: Estampar QR (sin firma digital)

### URL
```
POST /api/appestampadoqr
```

### Descripción
Agrega un estampado visual con código QR a un documento PDF sin firmarlo digitalmente. Útil para agregar marcas visuales de verificación o tracking.

### Headers
```
Content-Type: application/x-www-form-urlencoded
```

### Parámetros (Form Data)

| Parámetro | Tipo | Requerido | Descripción |
|-----------|------|-----------|-------------|
| `jwt` | string | Sí | Token JWT de autenticación |
| `documento` | string | Sí | Documento PDF codificado en Base64 |
| `json` | string | Sí | Metadatos en formato JSON (ver estructura abajo) |
| `base64` | string | Sí | Token de versión codificado en Base64 |

### Estructura del JSON de Metadatos

```json
{
  "nombreFirmante": "Juan Pérez García",      // REQUERIDO
  "razon": "Documento verificado",            // Opcional (default: "Documento verificado")
  "localizacion": "Quito, Ecuador",           // Opcional (default: "Ecuador")
  "infoQR": "https://verificacion.gob.ec/doc/12345", // Opcional (ej: URL de verificación)
  "pagina": 1,                                // Opcional (default: última página, -1)
  "posX": 50,                                 // Opcional (default: 50)
  "posY": 50,                                 // Opcional (default: 50)
  "ancho": 200,                               // Opcional (default: 200)
  "alto": 100                                 // Opcional (default: 100)
}
```

### Respuesta Exitosa

```json
{
  "resultado": "OK",
  "mensaje": "Estampado QR agregado exitosamente",
  "documentoEstampado": "BASE64_DEL_DOCUMENTO_ESTAMPADO..."
}
```

### Respuesta de Error

```json
{
  "resultado": "ERROR",
  "mensaje": "Descripción del error"
}
```

### Ejemplo con cURL

```bash
curl -X POST http://localhost:8080/api/appestampadoqr \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "jwt=TU_TOKEN_JWT" \
  -d "documento=BASE64_DEL_PDF" \
  -d "json={\"nombreFirmante\":\"Juan Pérez\",\"infoQR\":\"https://verify.com/123\"}" \
  -d "base64=TU_TOKEN_VERSION"
```

---

## 2. Endpoint: Firmar Documento con Estampado QR

### URL
```
POST /api/appfirmardocumentoconqr
```

### Descripción
**Nuevo endpoint que combina estampado QR y firma digital.** Primero agrega un estampado visual con código QR al documento y luego lo firma digitalmente. Este endpoint NO modifica el servicio de firma existente en producción (`/appfirmardocumento`).

### Flujo de Procesamiento
1. Decodifica el certificado PKCS#12
2. Extrae el nombre del firmante del certificado
3. **Estampa el documento con el código QR** (contiene información del firmante, razón, localización, fecha y datos adicionales)
4. **Firma digitalmente el documento estampado**
5. Retorna el documento estampado y firmado

### Headers
```
Content-Type: application/x-www-form-urlencoded
```

### Parámetros (Form Data)

| Parámetro | Tipo | Requerido | Descripción |
|-----------|------|-----------|-------------|
| `jwt` | string | Sí | Token JWT de autenticación |
| `pkcs12` | string | Sí | Certificado PKCS#12 codificado en Base64 |
| `password` | string | Sí | Contraseña del certificado |
| `documento` | string | Sí | Documento PDF codificado en Base64 |
| `json` | string | No | Metadatos en formato JSON (ver estructura abajo) |
| `base64` | string | Sí | Token de versión codificado en Base64 |

### Estructura del JSON de Metadatos

```json
{
  // Metadatos de la firma digital
  "razon": "Firma de aprobación",
  "localizacion": "Quito, Ecuador",
  "cargo": "Director General",
  
  // Metadatos del estampado QR
  "infoQR": "https://verificacion.gob.ec/doc/12345",
  "qrPagina": -1,      // -1 = última página, o número específico
  "qrPosX": 50,
  "qrPosY": 50,
  "qrAncho": 200,
  "qrAlto": 100
}
```

**Nota:** El `nombreFirmante` se extrae automáticamente del certificado (campo CN).

### Respuesta Exitosa

```json
{
  "resultado": "OK",
  "mensaje": "Documento estampado con QR y firmado digitalmente exitosamente",
  "documentoFirmado": "BASE64_DEL_DOCUMENTO_FIRMADO_Y_ESTAMPADO..."
}
```

### Respuesta de Error

```json
{
  "resultado": "ERROR",
  "mensaje": "Descripción del error"
}
```

### Ejemplo con cURL

```bash
curl -X POST http://localhost:8080/api/appfirmardocumentoconqr \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "jwt=TU_TOKEN_JWT" \
  -d "pkcs12=BASE64_DEL_CERTIFICADO" \
  -d "password=password_del_certificado" \
  -d "documento=BASE64_DEL_PDF" \
  -d "json={\"razon\":\"Firma de aprobación\",\"localizacion\":\"Quito, Ecuador\",\"infoQR\":\"https://verify.com/123\",\"qrPosX\":50,\"qrPosY\":50}" \
  -d "base64=TU_TOKEN_VERSION"
```

---

## Contenido del Código QR

El código QR generado contiene la siguiente información en formato de texto:

```
FIRMADO POR: [Nombre del firmante]
RAZON: [Razón de la firma]
LOCALIZACION: [Ubicación]
FECHA: [dd/MM/yyyy HH:mm:ss]
[Información adicional del campo infoQR]
```

---

## Visualización del Estampado

El estampado visual se compone de:

- **Izquierda:** Código QR cuadrado con la información
- **Derecha:** Texto con:
  - "Firmado electrónicamente por:"
  - Nombre del firmante (en negrita, tamaño grande)
  - "Validar únicamente con FirmaEC"

---

## Casos de Uso

### Caso 1: Solo Estampado Visual (sin firma digital)
- **Endpoint:** `/appestampadoqr`
- **Uso:** Agregar marcas de verificación, códigos de tracking, o información visual sin comprometer la integridad del documento con firma digital
- **Ejemplo:** Documentos que necesitan QR de verificación pero no requieren firma digital

### Caso 2: Estampado + Firma Digital
- **Endpoint:** `/appfirmardocumentoconqr`
- **Uso:** Agregar estampado visual con QR Y firma digital en un solo paso
- **Ejemplo:** Documentos oficiales que requieren tanto marca visual como validez jurídica de firma digital

### Caso 3: Firma Digital sin Estampado QR (existente)
- **Endpoint:** `/appfirmardocumento` (NO MODIFICADO)
- **Uso:** Mantiene el comportamiento actual para aplicaciones en producción
- **Ejemplo:** Sistemas legacy que ya usan el endpoint existente

---

## Posicionamiento del Estampado

### Coordenadas
- **posX:** Coordenada X desde la esquina inferior izquierda de la página (en puntos)
- **posY:** Coordenada Y desde la esquina inferior izquierda de la página (en puntos)
- **ancho:** Ancho total del área del estampado (en puntos)
- **alto:** Alto del área del estampado (en puntos)

### Valores por Defecto
```json
{
  "qrPosX": 50,     // 50 puntos desde la izquierda
  "qrPosY": 50,     // 50 puntos desde abajo
  "qrAncho": 200,   // 200 puntos de ancho total
  "qrAlto": 100     // 100 puntos de alto
}
```

### Nota sobre el Tamaño del QR
El código QR siempre será cuadrado con dimensiones iguales al `qrAlto`. El espacio restante (`qrAncho - qrAlto`) se usará para el texto informativo.

---

## Consideraciones de Seguridad

1. **Autenticación:** Ambos endpoints requieren token JWT válido
2. **Validación de Versión:** Requieren token de versión válido
3. **Certificado Digital:** El endpoint de firma valida la contraseña del certificado PKCS#12
4. **Límite de Tamaño:** Los archivos están sujetos al filtro `RequestSizeFilter`

---

## Diferencias con el Endpoint Existente

| Característica | `/appfirmardocumento` (existente) | `/appfirmardocumentoconqr` (nuevo) |
|----------------|-----------------------------------|-----------------------------------|
| Estampado QR | ❌ No | ✅ Sí |
| Firma Digital | ✅ Sí | ✅ Sí |
| Estado | ✅ En Producción | 🆕 Nuevo |
| Orden | Solo firma | 1. Estampa, 2. Firma |
| Metadatos QR | N/A | ✅ Soporta parámetros de QR |

---

## Colección de Postman

Los nuevos endpoints han sido agregados a la colección de Postman `Firma-Digital-API.postman_collection.json`:

- **Sección 3:** "Firma de Documentos" → incluye "Firmar Documento con QR"
- **Sección 5:** "Estampado Visual" → incluye "Estampar QR sin Firma"

---

## Archivos Involucrados

### Nuevos Archivos
- `/firmadigital-api/src/main/java/ec/gob/firmadigital/api/ServicioAppEstampadoQR.java`
- `/firmadigital-api/src/main/java/ec/gob/firmadigital/api/ServicioAppFirmarDocumentoConQR.java`

### Archivos Modificados
- `Firma-Digital-API.postman_collection.json`

### Archivos NO Modificados (Producción Segura)
- ✅ `ServicioAppFirmarDocumento.java` (endpoint original intacto)
- ✅ `ServicioAppFirmarDocumentoTransversal.java` (endpoint transversal intacto)

---

## Logs y Debugging

Ambos endpoints generan logs detallados:
- Decodificación de Base64
- Procesamiento de metadatos
- Generación de código QR
- Posicionamiento del estampado
- Proceso de firma digital (cuando aplica)

**Nivel de log:** `INFO` para operaciones normales, `WARNING` para errores recuperables, `SEVERE` para errores críticos.

---

## Fecha de Implementación
12 de diciembre de 2025
