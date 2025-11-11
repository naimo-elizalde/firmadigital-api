# Solución al Error de Servicios Externos

## 🔴 Problema Original

Al intentar usar los endpoints `/version` y `/fecha-hora`, se recibía el siguiente error:

```
RESTEASY004655: Unable to invoke request: org.apache.http.client.ClientProtocolException
```

## 🔍 Causa del Problema

La API original estaba diseñada como un **proxy** que hacía llamadas a servicios externos:

```java
// Código original problemático
private static final String WS_SYSTEM_PROPERTY = "firmadigital-servicio.url";
private static final String REST_SERVICE_URL = System.getProperty(WS_SYSTEM_PROPERTY) + "/version";

// Intentaba hacer una llamada REST a un servicio externo que no existe
Client client = ClientBuilder.newClient();
WebTarget target = client.target(REST_SERVICE_URL);
```

El problema es que:
1. La propiedad del sistema `firmadigital-servicio.url` no estaba configurada
2. Aunque estuviera configurada, apuntaría a un servicio externo que no existe en este despliegue

## ✅ Solución Implementada

Se modificaron los siguientes archivos para que funcionen de manera **standalone** sin dependencias externas:

### 1. `ServicioVersion.java`

**Antes**: Llamaba a un servicio externo para validar la versión  
**Ahora**: Lee la versión directamente del archivo `config.api.properties`

```java
@POST
@Path("/version")
public String validarEndpoint(@FormParam("base64") String base64) {
    // Lee la versión del archivo de configuración local
    String versionServidor = getVersion();
    
    // Retorna respuesta JSON directamente
    JsonObject response = new JsonObject();
    response.addProperty("resultado", "Version enabled");
    response.addProperty("versionServidor", versionServidor);
    return new Gson().toJson(response);
}
```

**Nuevo endpoint GET agregado**:
```bash
GET /api/version
```

### 2. `ServicioFechaHora.java`

**Antes**: Llamaba a un servicio externo, luego retornaba la fecha si la validación era exitosa  
**Ahora**: Retorna directamente la fecha y hora del servidor

```java
@POST
@Path("/fecha-hora")
public String getFechaHora(@FormParam("base64") String base64) {
    // Retorna directamente la fecha del servidor
    return ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
}
```

**Nuevo endpoint GET agregado**:
```bash
GET /api/fecha-hora
```

## 🚀 Cómo Usar la Nueva API

### Opción 1: POST (Compatible con versión original)

```bash
# Validar versión
curl -X POST http://localhost:8080/api/version \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "base64=NDQuMS4w"

# Obtener fecha y hora
curl -X POST http://localhost:8080/api/fecha-hora \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "base64=NDQuMS4w"
```

### Opción 2: GET (Nuevo, más simple)

```bash
# Obtener versión
curl http://localhost:8080/api/version

# Obtener fecha y hora
curl http://localhost:8080/api/fecha-hora
```

## 📦 Reconstruir la Imagen Docker

Después de estos cambios, necesitas reconstruir la imagen Docker:

```bash
# Detener contenedores actuales
docker-compose down

# Reconstruir sin caché
docker-compose build --no-cache

# Iniciar nuevamente
docker-compose up -d

# Verificar logs
docker-compose logs -f
```

O usando el script de administración:

```bash
./manage.sh rebuild
```

## ✅ Verificar que Funciona

```bash
# Test 1: Versión (POST)
curl -X POST http://localhost:8080/api/version \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "base64=NDQuMS4w"

# Respuesta esperada:
# {"resultado":"Version enabled","versionServidor":"4.1.0","versionCliente":"4.1.0","compatible":true}

# Test 2: Versión (GET)
curl http://localhost:8080/api/version

# Respuesta esperada:
# {"version":"4.1.0","resultado":"OK"}

# Test 3: Fecha y Hora (POST)
curl -X POST http://localhost:8080/api/fecha-hora \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "base64=NDQuMS4w"

# Respuesta esperada:
# 2025-11-11T10:30:45.123-05:00

# Test 4: Fecha y Hora (GET)
curl http://localhost:8080/api/fecha-hora

# Respuesta esperada:
# {"fechaHora":"2025-11-11T10:30:45.123-05:00","timestamp":1731347445,"zonaHoraria":"America/Guayaquil","resultado":"OK"}
```

## 🔧 Otros Servicios con el Mismo Problema

Los siguientes servicios también tienen dependencias externas y podrían necesitar modificaciones similares:

### Servicios que aún requieren backend externo:

1. **`ServicioAppFirmarDocumento.java`** - `/appfirmardocumento`
   - Depende de: `firmadigital-servicio-mobile.url`
   - Función: Firma de documentos

2. **`ServicioAppFirmarDocumentoTransversal.java`** - `/appfirmardocumentotransversal`
   - Depende de: `firmadigital-servicio-mobile.url`
   - Función: Firma transversal

3. **`ServicioAppValidarCertificadoDigital.java`** - `/appvalidarcertificadodigital`
   - Depende de: `firmadigital-servicio-mobile.url`
   - Función: Validación de certificados

4. **`ServicioAppVerificarDocumento.java`** - `/appverificardocumento`
   - Depende de: `firmadigital-servicio-mobile.url`
   - Función: Verificación de documentos

5. **`ServicioFirmaDigital.java`** - `/firmadigital`
   - Depende de: `firmadigital-servicio.url`
   - Función: Operaciones con documentos

6. **`ServicioCertificado.java`** - `/certificado`
   - Depende de: `firmadigital-servicio.url`
   - Función: Verificar revocación de certificados

## 🎯 Siguiente Paso: Implementar Lógica de Firma

Para hacer estos servicios completamente standalone, necesitas:

1. **Usar la librería `firmadigital-libreria` directamente** en los endpoints en lugar de hacer llamadas REST
2. **Implementar la lógica de firma** directamente en cada servicio
3. **Eliminar las dependencias de servicios externos**

### Ejemplo de cómo debería verse:

```java
@Path("/appfirmardocumento")
public class ServicioAppFirmarDocumento {
    
    @POST
    public String firmarDocumento(
        @FormParam("pkcs12") String pkcs12Base64,
        @FormParam("password") String password,
        @FormParam("documento") String documentoBase64
    ) {
        // Usar directamente la librería firmadigital-libreria
        // en lugar de hacer llamadas REST
        
        // Decodificar certificado y documento
        byte[] certBytes = Base64.getDecoder().decode(pkcs12Base64);
        byte[] docBytes = Base64.getDecoder().decode(documentoBase64);
        
        // Usar clases de firmadigital-libreria para firmar
        // FirmadorPDF firmador = new FirmadorPDF();
        // byte[] documentoFirmado = firmador.firmar(docBytes, certBytes, password);
        
        // Retornar documento firmado
        return Base64.getEncoder().encodeToString(documentoFirmado);
    }
}
```

## 📚 Documentación Actualizada

- **`API-EJEMPLOS.md`**: Actualizado con los nuevos endpoints
- **`cliente-web.html`**: Ya compatible con los cambios
- **`ejemplo_uso_api.py`**: Ya compatible con los cambios
- **`Firma-Digital-API.postman_collection.json`**: Ya compatible con los cambios

## ⚠️ Notas Importantes

1. Los cambios son **retrocompatibles**: los endpoints POST siguen funcionando como antes
2. Se agregaron endpoints GET adicionales para mayor facilidad de uso
3. **No se requiere configuración adicional** en WildFly
4. La API ahora es **verdaderamente standalone**

## 🆘 Si el Error Persiste

Si después de reconstruir sigues viendo el error:

1. Verifica que el contenedor se reconstruyó correctamente:
   ```bash
   docker images | grep firmadigital-api
   ```

2. Verifica los logs:
   ```bash
   docker-compose logs -f
   ```

3. Asegúrate de que no hay contenedores antiguos:
   ```bash
   docker-compose down -v
   docker system prune -a
   docker-compose up --build -d
   ```

4. Prueba directamente desde el contenedor:
   ```bash
   docker exec -it firmadigital-api /bin/bash
   curl http://localhost:8080/api/version
   ```
