# Firma Digital - Despliegue con Docker

Este directorio contiene los archivos necesarios para desplegar la API de Firma Digital junto con su librería en un contenedor Docker usando WildFly.

## Estructura del Proyecto

```
firmador/
├── firmadigital-api/          # API REST para firma digital
├── firmadigital-libreria/     # Librería de firma digital
├── Dockerfile                 # Definición del contenedor
├── docker-compose.yml         # Orquestación de servicios
├── .dockerignore             # Archivos excluidos del build
└── README-DOCKER.md          # Este archivo
```

## Características

- **Build Multi-Stage**: Optimiza el tamaño de la imagen final
- **Sin dependencias externas**: Todo se compila y empaqueta internamente
- **Java 17**: Compatible con las últimas características de Java
- **WildFly 31.0.1**: Servidor de aplicaciones Jakarta EE 10
- **Maven 3.9**: Para la compilación de los proyectos

## Construcción de la Imagen

### Opción 1: Usar Docker directamente

```bash
# Construir la imagen
docker build -t firmadigital-api:latest .

# Ejecutar el contenedor
docker run -d \
  --name firmadigital-api \
  -p 8080:8080 \
  -p 9990:9990 \
  firmadigital-api:latest
```

### Opción 2: Usar Docker Compose (Recomendado)

```bash
# Construir y levantar los servicios
docker-compose up -d

# Ver los logs
docker-compose logs -f

# Detener los servicios
docker-compose down
```

## Acceso a la Aplicación

Una vez que el contenedor esté en ejecución:

- **API**: http://localhost:8080/api
- **Consola de Administración WildFly**: http://localhost:9990
  - Usuario: `admin`
  - Contraseña: `Admin#2024`

## Verificación del Despliegue

```bash
# Verificar que el contenedor está corriendo
docker ps | grep firmadigital

# Ver los logs del contenedor
docker logs firmadigital-api

# Verificar el estado de la aplicación
curl http://localhost:8080/api
```

## Proceso de Build

El Dockerfile realiza los siguientes pasos:

1. **Stage 1 - Builder**:
   - Copia el código fuente de la librería
   - Compila e instala la librería en el repositorio local de Maven
   - Copia el código fuente de la API
   - Compila la API (que usa la librería previamente instalada)
   - Genera el archivo WAR

2. **Stage 2 - Runtime**:
   - Usa una imagen base de WildFly
   - Copia el WAR compilado al directorio de despliegues
   - Copia los archivos de configuración
   - Configura el usuario administrador
   - Expone los puertos necesarios

## Variables de Entorno

Puedes personalizar las siguientes variables en `docker-compose.yml`:

```yaml
environment:
  - JAVA_OPTS=-Xms512m -Xmx2048m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m
```

## Volúmenes

Los logs se persisten en el directorio local `./logs`:

```yaml
volumes:
  - ./logs:/opt/jboss/wildfly/standalone/log
```

## Troubleshooting

### El contenedor no inicia

```bash
# Ver los logs detallados
docker logs firmadigital-api

# Entrar al contenedor para debugging
docker exec -it firmadigital-api /bin/bash
```

### La aplicación no responde

```bash
# Verificar que WildFly está escuchando
docker exec firmadigital-api netstat -tuln | grep 8080

# Verificar el despliegue
docker exec firmadigital-api ls -la /opt/jboss/wildfly/standalone/deployments/
```

### Reconstruir desde cero

```bash
# Detener y eliminar contenedores
docker-compose down

# Eliminar la imagen
docker rmi firmadigital-api:latest

# Reconstruir sin caché
docker-compose build --no-cache

# Levantar nuevamente
docker-compose up -d
```

## Notas Importantes

- **No hay llamados a servicios externos**: Todo el proceso de build es offline después de descargar las dependencias de Maven
- **La librería se compila primero**: Es un requisito ya que la API depende de ella
- **Puertos expuestos**: 
  - `8080`: Acceso HTTP a la API
  - `9990`: Consola de administración (opcional, puede ser removido en producción)

## Producción

Para despliegue en producción, considera:

1. Cambiar las credenciales del usuario administrador
2. Configurar HTTPS/TLS
3. Ajustar los límites de memoria según tus necesidades
4. Implementar health checks
5. Configurar un reverse proxy (nginx, traefik, etc.)

## Limpieza

```bash
# Detener y eliminar todo
docker-compose down -v

# Eliminar la imagen
docker rmi firmadigital-api:latest

# Limpiar recursos Docker no utilizados
docker system prune -a
```

## 📚 Documentación Adicional

Este proyecto incluye documentación completa y ejemplos de uso:

### 📖 Documentación de la API
- **`API-EJEMPLOS.md`**: Documentación completa con ejemplos de todas las peticiones
  - Ejemplos con cURL
  - Ejemplos con JavaScript/Fetch
  - Ejemplos con Postman
  - Descripciones detalladas de endpoints
  - Códigos de respuesta y errores

### 🔧 Herramientas de Prueba
- **`Firma-Digital-API.postman_collection.json`**: Colección de Postman
  - Importa este archivo en Postman para probar la API
  - Incluye todas las peticiones pre-configuradas
  - Variables de entorno configurables

- **`ejemplo_uso_api.py`**: Cliente Python de ejemplo
  - Script completo con todas las funciones
  - Requiere: `pip install requests`
  - Uso: `python ejemplo_uso_api.py`

- **`cliente-web.html`**: Cliente web interactivo
  - Abre este archivo en tu navegador
  - Interfaz gráfica para probar todos los endpoints
  - No requiere instalación

### 🚀 Comenzar a Usar la API

1. **Inicia el servidor Docker**:
   ```bash
   docker-compose up -d
   ```

2. **Elige tu herramienta de prueba**:
   - **Postman**: Importa `Firma-Digital-API.postman_collection.json`
   - **Python**: Ejecuta `python ejemplo_uso_api.py`
   - **Web**: Abre `cliente-web.html` en tu navegador
   - **cURL**: Consulta `API-EJEMPLOS.md` para comandos

3. **Lee la documentación**:
   ```bash
   cat API-EJEMPLOS.md
   ```
