#!/bin/bash
# ==============================================================================
# Script de Despliegue Automatizado en AWS EC2 - VidaSalud
# ==============================================================================

set -e

# Colores de salida
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Detección de IP Pública de la EC2
PUBLIC_IP=$(curl -s --connect-timeout 2 http://checkip.amazonaws.com || curl -s --connect-timeout 2 http://169.254.169.254/latest/meta-data/public-ipv4 || echo "52.1.200.223")

# Función: Configurar Swap
setup_swap() {
    echo -e "${YELLOW}>> Verificando memoria SWAP...${NC}"
    SWAP_TOTAL=$(free -m | awk '/Swap:/ {print $2}')
    if [ -z "$SWAP_TOTAL" ] || [ "$SWAP_TOTAL" -lt 2000 ]; then
        echo -e "${YELLOW}>> Configurando archivo Swap de 4GB para evitar bloqueos por RAM en EC2...${NC}"
        sudo swapoff -a 2>/dev/null || true
        sudo rm -f /swapfile
        sudo fallocate -l 4G /swapfile 2>/dev/null || sudo dd if=/dev/zero of=/swapfile bs=1M count=4096
        sudo chmod 600 /swapfile
        sudo mkswap /swapfile
        sudo swapon /swapfile || true
        if ! grep -q '/swapfile' /etc/fstab; then
            echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab > /dev/null
        fi
        echo -e "${GREEN}>> Memoria Swap de 4GB activada con éxito.${NC}"
    else
        echo -e "${GREEN}>> Memoria Swap ya activa ($SWAP_TOTAL MB detectados).${NC}"
    fi
    free -h
}

# Función: Detener solo microservicios Java de VidaSalud (sin matar Docker ni Kafka-UI)
stop_microservices() {
    echo -e "${YELLOW}>> Deteniendo microservicios Spring Boot de VidaSalud...${NC}"
    pkill -9 -f 'ms-vidasalud' 2>/dev/null || true
    pkill -9 -f 'ms_vidasalud' 2>/dev/null || true
    echo -e "${GREEN}>> Microservicios Java detenidos y puertos liberados.${NC}"
}

# Función: Limpiar y detener todo (Microservicios + Docker + Logs)
clean_all() {
    echo -e "${YELLOW}>> Deteniendo microservicios Spring Boot...${NC}"
    stop_microservices
    echo -e "${YELLOW}>> Deteniendo contenedores Docker...${NC}"
    docker compose -f infra/docker-compose.yml down 2>/dev/null || docker compose down 2>/dev/null || true
    echo -e "${YELLOW}>> Limpiando archivos de log antiguos...${NC}"
    rm -f *.log
    echo -e "${GREEN}>> Sistema completamente limpio y memoria liberada.${NC}"
}

# Función: Levantar Infraestructura Docker (PostgreSQL, RabbitMQ Clúster, Kafka KRaft, Kafka-UI)
start_infrastructure() {
    echo -e "${YELLOW}>> Levantando contenedores Docker (Postgres, RabbitMQ Cluster, Kafka KRaft, Kafka-UI)...${NC}"
    docker compose -f infra/docker-compose.yml up -d
    
    echo -e "${YELLOW}>> Esperando inicio de PostgreSQL para asegurar bases de datos...${NC}"
    for i in {1..20}; do
        if docker exec postgres-vidasalud pg_isready -U postgres >/dev/null 2>&1; then
            echo -e "${GREEN}>> PostgreSQL está listo.${NC}"
            break
        fi
        sleep 2
    done

    echo -e "${YELLOW}>> Asegurando existencia de bases de datos de microservicios...${NC}"
    docker exec -i postgres-vidasalud psql -U postgres -c "CREATE DATABASE vidasalud_catalog;" 2>/dev/null || true
    docker exec -i postgres-vidasalud psql -U postgres -c "CREATE DATABASE vidasalud_appointments;" 2>/dev/null || true
    docker exec -i postgres-vidasalud psql -U postgres -c "CREATE DATABASE vidasalud_audit;" 2>/dev/null || true
    docker exec -i postgres-vidasalud psql -U postgres -c "CREATE DATABASE vidasalud_report;" 2>/dev/null || true
    echo -e "${GREEN}>> Infraestructura de datos y mensajería lista.${NC}"
}

# Función: Compilar microservicios
compile_microservices() {
    echo -e "${YELLOW}>> Compilando microservicios individualmente con Maven (Java 21)...${NC}"
    for ms in ms-vidasalud-catalog ms-vidasalud-appointments ms-vidasalud-audit ms-vidasalud-report ms-vidasalud-notify; do
        if [ -d "$ms" ]; then
            echo -e "${CYAN}>> Compilando $ms...${NC}"
            (cd "$ms" && ./mvnw clean package -DskipTests -q)
        fi
    done
    echo -e "${GREEN}>> Compilación completada con éxito.${NC}"
}

# Función: Iniciar microservicios de manera escalonada (evita saturar la CPU)
start_microservices() {
    stop_microservices
    
    echo -e "${YELLOW}>> Iniciando microservicios secuencialmente con límite de memoria (-Xmx180m)...${NC}"
    
    echo -e "${CYAN}1/5 Iniciando Catálogo (Puerto 8081)...${NC}"
    nohup java -Xmx180m -jar ms-vidasalud-catalog/target/*.jar > catalog.log 2>&1 &
    sleep 8

    echo -e "${CYAN}2/5 Iniciando Citas y Atenciones (Puerto 8082)...${NC}"
    nohup java -Xmx180m -jar ms-vidasalud-appointments/target/*.jar > appointments.log 2>&1 &
    sleep 8

    echo -e "${CYAN}3/5 Iniciando Auditoría (Puerto 8083)...${NC}"
    nohup java -Xmx180m -jar ms-vidasalud-audit/target/*.jar > audit.log 2>&1 &
    sleep 6

    echo -e "${CYAN}4/5 Iniciando Reportes (Puerto 8084)...${NC}"
    nohup java -Xmx180m -jar ms-vidasalud-report/target/*.jar > report.log 2>&1 &
    sleep 6

    echo -e "${CYAN}5/5 Iniciando Notificaciones (Puerto 8085)...${NC}"
    nohup java -Xmx180m -jar ms-vidasalud-notify/target/*.jar > notify.log 2>&1 &
    
    echo -e "${YELLOW}>> Esperando estabilización de puertos de Spring Boot...${NC}"
    for i in {1..20}; do
        if curl -s http://localhost:8081/catalog/boxes >/dev/null 2>&1; then
            echo -e "${GREEN}>> Backend respondiendo en puerto 8081.${NC}"
            break
        fi
        sleep 2
    done
}

# Función: Ver Estado y Logs
show_status() {
    echo ""
    echo -e "${BLUE}=================== CONTENEDORES DOCKER ===================${NC}"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    echo ""
    echo -e "${BLUE}================== MICROSERVICIOS BACKEND ==================${NC}"
    ps aux | grep -E 'ms-vidasalud|ms_vidasalud' | grep -v grep || echo "No hay microservicios Java activos."
    echo ""
    echo -e "${BLUE}================== VERIFICACION DE PUERTOS ================${NC}"
    for port in 8081 8082 8083 8084 8085 15672 8088; do
        if nc -z localhost $port 2>/dev/null || curl -s http://localhost:$port >/dev/null 2>&1; then
            echo -e "  Puerto $port: ${GREEN}ONLINE${NC}"
        else
            echo -e "  Puerto $port: ${RED}OFFLINE${NC}"
        fi
    done
}

# ==============================================================================
# MENÚ PRINCIPAL INTERACTIVO
# ==============================================================================
clear || true
echo -e "${BLUE}==============================================================================${NC}"
echo -e "${BOLD}${GREEN}        🏥 VidaSalud - Centro de Control de Despliegue en AWS EC2     ${NC}"
echo -e "${BLUE}==============================================================================${NC}"
echo -e "IP Pública Detectada: ${CYAN}${PUBLIC_IP}${NC}"
echo ""
echo -e "${BOLD}Seleccione una opción:${NC}"
echo -e "  ${GREEN}1)${NC} ${BOLD}Ejecutar TODO (Despliegue Completo en 1 Clic)${NC}"
echo -e "     (Swap + Limpieza + Docker Infra + Bases de Datos + Compilar + Backend)"
echo -e "  ${GREEN}2)${NC} ${BOLD}Configurar / Aumentar Memoria SWAP (4 GB)${NC}"
echo -e "  ${GREEN}3)${NC} ${BOLD}Detener Procesos Antiguos y Dejar Limpio${NC} (liberar RAM y puertos)"
echo -e "  ${GREEN}4)${NC} ${BOLD}Levantar Infraestructura Docker${NC} (Postgres, RabbitMQ Clúster, Kafka, Kafka-UI)"
echo -e "  ${GREEN}5)${NC} ${BOLD}Compilar y Levantar Microservicios Backend${NC} (Java Spring Boot)"
echo -e "  ${GREEN}6)${NC} ${BOLD}Ver Estado del Sistema y Puertos${NC}"
echo -e "  ${GREEN}7)${NC} ${BOLD}Ver Logs en Tiempo Real${NC} (catalog, appointments, audit, report, notify)"
echo -e "  ${RED}q)${NC} Salir"
echo ""
read -p "Ingrese una opción [1-7/q]: " OPTION

case $OPTION in
    1)
        setup_swap
        clean_all
        start_infrastructure
        compile_microservices
        start_microservices
        ;;
    2)
        setup_swap
        exit 0
        ;;
    3)
        echo ""
        echo "a) Detener solo microservicios backend Java (mantiene Docker vivo)"
        echo "b) Detener todo (Java + Docker + borrar logs)"
        read -p "Seleccione [a/b]: " CLEAN_OPT
        if [ "$CLEAN_OPT" = "a" ] || [ "$CLEAN_OPT" = "A" ]; then
            stop_microservices
        else
            clean_all
        fi
        exit 0
        ;;
    4)
        setup_swap
        start_infrastructure
        ;;
    5)
        stop_microservices
        echo "¿Desea compilar antes de iniciar? (s/n): "
        read RECOMPILE
        if [ "$RECOMPILE" = "s" ] || [ "$RECOMPILE" = "S" ]; then
            compile_microservices
        fi
        start_microservices
        ;;
    6)
        show_status
        exit 0
        ;;
    7)
        echo "Seleccione el log que desea inspeccionar:"
        echo "1) catalog.log"
        echo "2) appointments.log"
        echo "3) audit.log"
        echo "4) report.log"
        echo "5) notify.log"
        read -p "Opción [1-5]: " LOG_OPT
        case $LOG_OPT in
            1) tail -n 50 -f catalog.log ;;
            2) tail -n 50 -f appointments.log ;;
            3) tail -n 50 -f audit.log ;;
            4) tail -n 50 -f report.log ;;
            5) tail -n 50 -f notify.log ;;
            *) echo "Opción no válida." ;;
        esac
        exit 0
        ;;
    q|Q)
        echo "Operación cancelada."
        exit 0
        ;;
    *)
        echo -e "${RED}Opción no reconocida.${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}==============================================================================${NC}"
echo -e "${GREEN}                    ✅ OPERACIÓN FINALIZADA CON ÉXITO                         ${NC}"
echo -e "${GREEN}==============================================================================${NC}"
echo -e "Accesos de evaluación:"
echo -e "  🌐 Panel Kafka-UI (3 réplicas/particiones):  ${BLUE}http://${PUBLIC_IP}:8088${NC}"
echo -e "  🐇 RabbitMQ Nodo 1 (Clúster / 3 Colas/DLQs): ${BLUE}http://${PUBLIC_IP}:15672${NC} (guest / guest)"
echo -e "  🐇 RabbitMQ Nodo 2 (Clúster Alta Dispon.):   ${BLUE}http://${PUBLIC_IP}:15673${NC} (guest / guest)"
echo -e "  📅 Microservicio Citas:                      ${BLUE}http://${PUBLIC_IP}:8082/appointments${NC}"
echo -e "  🗂️ Microservicio Catálogo:                   ${BLUE}http://${PUBLIC_IP}:8081/catalog/boxes${NC}"
echo -e "  🔒 Microservicio Auditoría:                  ${BLUE}http://${PUBLIC_IP}:8083/audit/auditorias${NC}"
echo -e "  📊 Microservicio Reportería:                 ${BLUE}http://${PUBLIC_IP}:8084/report/health${NC}"
echo -e "${GREEN}==============================================================================${NC}"
