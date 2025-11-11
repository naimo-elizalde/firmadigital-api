#!/bin/bash
###############################################################################
# Script de administración para Firma Digital API
# Facilita operaciones comunes con Docker
###############################################################################

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Variables
PROJECT_NAME="firmadigital-api"
COMPOSE_FILE="docker-compose.yml"
API_URL="http://localhost:8080/api"

# Funciones de utilidad
print_header() {
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# Función para verificar si Docker está instalado
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker no está instalado. Por favor, instala Docker primero."
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose no está instalado. Por favor, instala Docker Compose primero."
        exit 1
    fi
}

# Función para construir la imagen
build() {
    print_header "Construyendo imagen Docker"
    docker-compose build --no-cache
    print_success "Imagen construida exitosamente"
}

# Función para iniciar los servicios
start() {
    print_header "Iniciando servicios"
    docker-compose up -d
    print_success "Servicios iniciados"
    print_info "API disponible en: ${API_URL}"
    print_info "Consola WildFly: http://localhost:9990"
    print_info "Usuario: admin / Contraseña: Admin#2024"
}

# Función para detener los servicios
stop() {
    print_header "Deteniendo servicios"
    docker-compose down
    print_success "Servicios detenidos"
}

# Función para reiniciar los servicios
restart() {
    print_header "Reiniciando servicios"
    stop
    start
}

# Función para ver logs
logs() {
    print_header "Mostrando logs (Ctrl+C para salir)"
    docker-compose logs -f
}

# Función para ver el estado
status() {
    print_header "Estado de los servicios"
    docker-compose ps
}

# Función para entrar al contenedor
shell() {
    print_header "Accediendo al contenedor"
    print_info "Escribe 'exit' para salir"
    docker exec -it ${PROJECT_NAME} /bin/bash
}

# Función para verificar la salud de la API
health() {
    print_header "Verificando estado de la API"
    
    # Verificar si el contenedor está corriendo
    if ! docker ps | grep -q ${PROJECT_NAME}; then
        print_error "El contenedor no está en ejecución"
        exit 1
    fi
    
    print_info "Contenedor en ejecución ✓"
    
    # Verificar conexión a la API
    print_info "Probando conexión a la API..."
    
    if curl -s -o /dev/null -w "%{http_code}" ${API_URL}/version > /dev/null 2>&1; then
        print_success "API respondiendo correctamente"
    else
        print_warning "API no responde. Puede estar iniciándose..."
        print_info "Espera unos segundos y vuelve a intentar"
    fi
}

# Función para limpiar todo
clean() {
    print_header "Limpiando recursos Docker"
    print_warning "Esto eliminará contenedores, imágenes y volúmenes"
    read -p "¿Estás seguro? (s/n) " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Ss]$ ]]; then
        docker-compose down -v
        docker rmi ${PROJECT_NAME}:latest 2>/dev/null || true
        print_success "Recursos limpiados"
    else
        print_info "Operación cancelada"
    fi
}

# Función para rebuild completo
rebuild() {
    print_header "Reconstruyendo desde cero"
    stop
    docker-compose build --no-cache
    start
    print_success "Rebuild completado"
}

# Función para backup de logs
backup_logs() {
    print_header "Creando backup de logs"
    
    BACKUP_DIR="./backups/logs"
    TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
    BACKUP_FILE="${BACKUP_DIR}/logs_${TIMESTAMP}.tar.gz"
    
    mkdir -p ${BACKUP_DIR}
    
    docker exec ${PROJECT_NAME} tar czf - /opt/jboss/wildfly/standalone/log > ${BACKUP_FILE}
    
    print_success "Backup guardado en: ${BACKUP_FILE}"
}

# Función para mostrar información del sistema
info() {
    print_header "Información del Sistema"
    
    echo -e "${BLUE}Versión de Docker:${NC}"
    docker --version
    
    echo -e "\n${BLUE}Versión de Docker Compose:${NC}"
    docker-compose --version
    
    echo -e "\n${BLUE}Uso de recursos:${NC}"
    docker stats --no-stream ${PROJECT_NAME} 2>/dev/null || print_warning "Contenedor no está corriendo"
    
    echo -e "\n${BLUE}Imágenes locales:${NC}"
    docker images | grep ${PROJECT_NAME}
}

# Función para abrir el cliente web
open_client() {
    print_header "Abriendo cliente web"
    
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        open cliente-web.html
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        # Linux
        xdg-open cliente-web.html 2>/dev/null || print_info "Abre manualmente: cliente-web.html"
    else
        print_info "Abre manualmente el archivo: cliente-web.html"
    fi
}

# Función de ayuda
show_help() {
    cat << EOF
${BLUE}Firma Digital API - Script de Administración${NC}

${GREEN}Uso:${NC}
    ./manage.sh [comando]

${GREEN}Comandos disponibles:${NC}
    build       - Construir la imagen Docker
    start       - Iniciar los servicios
    stop        - Detener los servicios
    restart     - Reiniciar los servicios
    logs        - Ver logs en tiempo real
    status      - Ver estado de los servicios
    shell       - Acceder al contenedor
    health      - Verificar estado de la API
    clean       - Limpiar recursos Docker
    rebuild     - Reconstruir desde cero
    backup      - Crear backup de logs
    info        - Mostrar información del sistema
    client      - Abrir cliente web de prueba
    help        - Mostrar esta ayuda

${GREEN}Ejemplos:${NC}
    ./manage.sh start       # Iniciar la API
    ./manage.sh logs        # Ver logs
    ./manage.sh health      # Verificar estado
    ./manage.sh rebuild     # Reconstruir todo

${GREEN}URLs:${NC}
    API:                ${API_URL}
    Consola WildFly:    http://localhost:9990
    Cliente Web:        ./cliente-web.html

${GREEN}Documentación:${NC}
    API-EJEMPLOS.md     - Documentación completa de la API
    README-DOCKER.md    - Guía de Docker
EOF
}

# Script principal
main() {
    check_docker
    
    case "${1:-help}" in
        build)
            build
            ;;
        start)
            start
            ;;
        stop)
            stop
            ;;
        restart)
            restart
            ;;
        logs)
            logs
            ;;
        status)
            status
            ;;
        shell)
            shell
            ;;
        health)
            health
            ;;
        clean)
            clean
            ;;
        rebuild)
            rebuild
            ;;
        backup)
            backup_logs
            ;;
        info)
            info
            ;;
        client)
            open_client
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            print_error "Comando desconocido: $1"
            echo
            show_help
            exit 1
            ;;
    esac
}

# Ejecutar script principal
main "$@"
