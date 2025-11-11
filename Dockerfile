# Dockerfile para firmadigital-api y firmadigital-libreria
# Build stage: Compila ambos proyectos usando Maven
FROM maven:3.9-eclipse-temurin-17 AS builder

# Establecer directorio de trabajo
WORKDIR /build

# Copiar archivos de la librería
COPY firmadigital-libreria/pom.xml /build/firmadigital-libreria/
COPY firmadigital-libreria/src /build/firmadigital-libreria/src

# Construir e instalar la librería en el repositorio local de Maven
WORKDIR /build/firmadigital-libreria
RUN mvn clean install -DskipTests

# Copiar archivos de la API
WORKDIR /build
COPY firmadigital-api/pom.xml /build/firmadigital-api/
COPY firmadigital-api/src /build/firmadigital-api/src

# Construir la API (que usará la librería desde el repo local de Maven)
WORKDIR /build/firmadigital-api
RUN mvn clean package -DskipTests

# Runtime stage: WildFly con la aplicación desplegada
FROM quay.io/wildfly/wildfly:31.0.1.Final-jdk17

# Variables de entorno para WildFly
ENV WILDFLY_HOME=/opt/jboss/wildfly
ENV DEPLOYMENT_DIR=${WILDFLY_HOME}/standalone/deployments

# Crear usuario administrador de WildFly (opcional, para acceso a consola)
RUN ${WILDFLY_HOME}/bin/add-user.sh admin Admin#2024 --silent

# Copiar el WAR compilado desde el stage de build
COPY --from=builder /build/firmadigital-api/target/api.war ${DEPLOYMENT_DIR}/

# Copiar archivos de configuración si existen
COPY --from=builder /build/firmadigital-api/src/main/resources/config.api.properties ${WILDFLY_HOME}/standalone/configuration/
COPY --from=builder /build/firmadigital-libreria/src/main/resources/config.rubrica.properties ${WILDFLY_HOME}/standalone/configuration/

# Exponer puertos
# 8080: HTTP
# 9990: Management Console
EXPOSE 8080 9990

# Comando para iniciar WildFly
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
