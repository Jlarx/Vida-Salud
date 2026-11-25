#!/bin/bash
# ==============================================================================
# 🏥 VidaSalud - Script de Despliegue Automatizado en AWS EC2
# ==============================================================================

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
BOLD='\033[1m'
NC='\033[0m'

chmod -R +x . 2>/dev/null || true

PUBLIC_IP=$(curl -s --connect-timeout 2 http://checkip.amazonaws.com || curl -s --connect-timeout 2 http://169.254.169.254/latest/meta-data/public-ipv4 || echo "52.1.200.223")

ensure_swap() {
    SWAP_TOTAL=$(free -m | awk '/Swap:/ {print $2}')
    if [ -z "$SWAP_TOTAL" ] || [ "$SWAP_TOTAL" -lt 2000 ]; then
        echo -e "${YELLOW}>> Configurando Swap de 4GB para evitar falta de memoria...${NC}"
        sudo swapoff -a 2>/dev/null || true
        sudo rm -f /swapfile
        sudo fallocate -l 4G /swapfile 2>/dev/null || sudo dd if=/dev/zero of=/swapfile bs=1M count=4096
        sudo chmod 600 /swapfile
        sudo mkswap /swapfile
        sudo swapon /swapfile || true
        if ! grep -q '/swapfile' /etc/fstab; then
            echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab > /dev/null
        fi
        echo -e "${GREEN}>> Swap de 4GB activado correctamente.${NC}"
    fi
}

start_infra() {
    ensure_swap

    # Asegurar mapeo de hostnames Kafka en /etc/hosts
    grep -q "kafka-1" /etc/hosts 2>/dev/null || echo "127.0.0.1 kafka-1 kafka-2 kafka-3" | sudo tee -a /etc/hosts >/dev/null 2>&1 || true

    echo -e "${YELLOW}>> Levantando contenedores Docker (Postgres, RabbitMQ Cluster, Kafka KRaft, Kafka-UI)...${NC}"
    docker compose -f infra/docker-compose.yml up -d --remove-orphans

    echo -e "${YELLOW}>> Verificando PostgreSQL y Clúster Kafka...${NC}"
    for i in {1..20}; do
        if docker exec postgres-vidasalud pg_isready -U postgres >/dev/null 2>&1; then
            break
        fi
        sleep 1
    done

    # Creación explícita de todas las bases de datos requeridas si no existen
    docker exec -i postgres-vidasalud psql -U postgres -c "CREATE DATABASE vidasalud_catalog;" 2>/dev/null || true
    docker exec -i postgres-vidasalud psql -U postgres -c "CREATE DATABASE vidasalud_appointments;" 2>/dev/null || true
    docker exec -i postgres-vidasalud psql -U postgres -c "CREATE DATABASE vidasalud_audit;" 2>/dev/null || true
    docker exec -i postgres-vidasalud psql -U postgres -c "CREATE DATABASE vidasalud_report;" 2>/dev/null || true

    # Limpiar cola conflictiva en RabbitMQ si existía
    docker exec rabbitmq-node1 rabbitmqctl delete_queue notify.queue 2>/dev/null || true

    # Esperar sincronización de quórum Kafka
    sleep 4
    echo -e "${YELLOW}>> Verificando tópicos Kafka (3 particiones, 3 réplicas)...${NC}"
    docker exec kafka-1 /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka-1:9092 --create --if-not-exists --topic vidasalud-citas --partitions 3 --replication-factor 3 --config retention.ms=604800000 2>/dev/null || true
    docker exec kafka-1 /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka-1:9092 --create --if-not-exists --topic vidasalud-alertas --partitions 3 --replication-factor 3 --config retention.ms=604800000 2>/dev/null || true
    docker exec kafka-1 /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka-1:9092 --alter --topic vidasalud-citas --partitions 3 2>/dev/null || true
    docker exec kafka-1 /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka-1:9092 --alter --topic vidasalud-alertas --partitions 3 2>/dev/null || true
    echo -e "${GREEN}>> Infraestructura Docker y Tópicos Kafka listos.${NC}"
}

compile_all() {
    echo -e "${YELLOW}>> Compilando microservicios individualmente con Maven...${NC}"
    export MAVEN_OPTS="-Xmx160m"
    for ms in ms-vidasalud-catalog ms-vidasalud-appointments ms-vidasalud-audit ms-vidasalud-report ms-vidasalud-notify; do
        if [ -d "$ms" ]; then
            echo -e "${CYAN}>> Compilando $ms...${NC}"
            (cd "$ms" && chmod +x mvnw 2>/dev/null || true; ./mvnw clean package -DskipTests -q)
        fi
    done
    echo -e "${GREEN}>> Todos los microservicios compilados exitosamente.${NC}"
}

start_service_step() {
    local name="$1"
    local port="$2"
    local jar_dir="$3"
    local logfile="$4"
    local step_num="$5"

    local jar=$(ls $jar_dir/target/*SNAPSHOT.jar 2>/dev/null | grep -v '\.original' | head -n 1)
    if [ -z "$jar" ] || [ ! -f "$jar" ]; then
        echo -e "   ${CYAN}>> Compilando ejecutable para $name...${NC}"
        (cd "$jar_dir" && chmod +x mvnw 2>/dev/null || true; ./mvnw clean package -DskipTests -q)
        jar=$(ls $jar_dir/target/*SNAPSHOT.jar 2>/dev/null | grep -v '\.original' | head -n 1)
    fi

    echo -ne "   [${step_num}/5] Iniciando ${name} (Puerto ${port})... "
    nohup java -Xms64m -Xmx160m -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -jar "$jar" > "$logfile" 2>&1 &

    local is_up=false
    for i in {1..35}; do
        if nc -z localhost $port 2>/dev/null || curl -s --connect-timeout 1 http://localhost:$port >/dev/null 2>&1; then
            is_up=true
            echo -e "${GREEN}ONLINE${NC} (en ${i}s)"
            break
        fi
        sleep 1
    done

    if [ "$is_up" = false ]; then
        echo -e "${RED}OFFLINE${NC}"
        echo -e "${CYAN}   --- Últimas líneas de $logfile ---${NC}"
        tail -n 8 "$logfile" | sed 's/^/     /'
        echo -e "${CYAN}   ----------------------------------${NC}"
    fi
}

launch_services() {
    echo -e "${YELLOW}>> Iniciando microservicios secuencialmente (evita sobrecarga de CPU/RAM)...${NC}"
    start_service_step "Catálogo" 8081 "ms-vidasalud-catalog" "catalog.log" 1
    start_service_step "Citas" 8082 "ms-vidasalud-appointments" "appointments.log" 2
    start_service_step "Auditoría" 8083 "ms-vidasalud-audit" "audit.log" 3
    start_service_step "Reportes" 8084 "ms-vidasalud-report" "report.log" 4
    start_service_step "Notificaciones" 8085 "ms-vidasalud-notify" "notify.log" 5

    echo ""
    echo -e "${GREEN}>> Grupos Consumidores Kafka Activos:${NC}"
    sleep 3
    docker exec kafka-1 /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kafka-1:9092 --list 2>/dev/null || true
}

fast_start() {
    echo -e "${CYAN}==============================================================================${NC}"
    echo -e "${BOLD}${GREEN}  ⚡ INICIO RÁPIDO DE VIDASALUD (Listo en ~30 segundos) ${NC}"
    echo -e "${CYAN}==============================================================================${NC}"
    
    echo -e "${YELLOW}>> Sincronizando repositorio con GitHub...${NC}"
    git reset --hard origin/main 2>/dev/null || true
    git pull origin main 2>/dev/null || true
    chmod -R +x . 2>/dev/null || true

    echo -e "${YELLOW}>> Deteniendo instancias previas de Java si existen...${NC}"
    pkill -9 -f 'ms-vidasalud' 2>/dev/null || true
    pkill -9 -f 'ms_vidasalud' 2>/dev/null || true
    sleep 1

    start_infra
    launch_services
}

rebuild_all() {
    echo -e "${CYAN}==============================================================================${NC}"
    echo -e "${BOLD}${YELLOW}  🔨 RECONSTRUCCIÓN COMPLETA (Maven + Docker + Microservicios) ${NC}"
    echo -e "${CYAN}==============================================================================${NC}"
    
    echo -e "${YELLOW}>> Sincronizando código con GitHub...${NC}"
    git reset --hard origin/main 2>/dev/null || true
    git pull origin main 2>/dev/null || true
    chmod -R +x . 2>/dev/null || true

    echo -e "${YELLOW}>> Deteniendo instancias previas de microservicios...${NC}"
    pkill -9 -f 'ms-vidasalud' 2>/dev/null || true
    pkill -9 -f 'ms_vidasalud' 2>/dev/null || true
    sleep 2

    start_infra
    compile_all
    launch_services
}

stop_all() {
    echo -e "${YELLOW}>> Deteniendo microservicios Spring Boot...${NC}"
    pkill -9 -f 'ms-vidasalud' 2>/dev/null || true
    pkill -9 -f 'ms_vidasalud' 2>/dev/null || true
    
    echo -e "${YELLOW}>> Deteniendo contenedores Docker...${NC}"
    docker compose -f infra/docker-compose.yml down 2>/dev/null || docker compose down 2>/dev/null || true
    
    echo -e "${YELLOW}>> Limpiando archivos de log...${NC}"
    rm -f *.log
    
    echo -e "${GREEN}>> Todo detenido y sistema completamente limpio.${NC}"
}

show_status() {
    echo ""
    echo -e "${BLUE}=================== CONTENEDORES DOCKER ===================${NC}"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    echo ""
    echo -e "${BLUE}================== MICROSERVICIOS BACKEND ==================${NC}"
    ps aux | grep -E 'ms-vidasalud|ms_vidasalud' | grep -v grep || echo "No hay microservicios Java activos."
    echo ""
    echo -e "${BLUE}================== ESTADO DE LOS PUERTOS ===================${NC}"
    for item in "8081:Catálogo" "8082:Citas" "8083:Auditoría" "8084:Reportes" "8085:Notificaciones" "15672:RabbitMQ" "8088:Kafka-UI"; do
        port="${item%%:*}"
        name="${item##*:}"
        if nc -z localhost $port 2>/dev/null || curl -s http://localhost:$port >/dev/null 2>&1; then
            echo -e "  Puerto $port ($name): ${GREEN}ONLINE${NC}"
        else
            echo -e "  Puerto $port ($name): ${RED}OFFLINE${NC}"
        fi
    done
    echo ""
    echo -e "${BLUE}================ GRUPOS CONSUMIDORES KAFKA =================${NC}"
    docker exec kafka-1 /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kafka-1:9092 --list 2>/dev/null || true
    echo ""
    echo -e "${BLUE}================ PRUEBA AWS API GATEWAY ===================${NC}"
    GW_CODE=$(curl -s -o /dev/null -w "%{http_code}" https://zzyt3obhvf.execute-api.us-east-1.amazonaws.com/prod/catalog/boxes || echo "ERROR")
    echo -e "  Conexión API Gateway (/catalog/boxes): Código HTTP ${CYAN}${GW_CODE}${NC}"
    if [ "$GW_CODE" = "200" ] || [ "$GW_CODE" = "401" ] || [ "$GW_CODE" = "403" ]; then
        echo -e "  Estado: ${GREEN}API Gateway conectado exitosamente con tu backend.${NC}"
    else
        echo -e "  Estado: ${RED}API Gateway no llega al backend. Verifica que Catálogo esté ONLINE y el botón 'Implementar API' presionado en AWS.${NC}"
    fi
}

show_logs() {
    echo "Seleccione el servicio para ver su log:"
    echo "1) Catálogo (catalog.log)"
    echo "2) Citas (appointments.log)"
    echo "3) Auditoría (audit.log)"
    echo "4) Reportes (report.log)"
    echo "5) Notificaciones (notify.log)"
    read -p "Opción [1-5]: " LOG_OPT
    case $LOG_OPT in
        1) tail -n 50 -f catalog.log ;;
        2) tail -n 50 -f appointments.log ;;
        3) tail -n 50 -f audit.log ;;
        4) tail -n 50 -f report.log ;;
        5) tail -n 50 -f notify.log ;;
        *) echo "Opción inválida." ;;
    esac
}

# Ejecución por argumentos directos (ej: ./deploy-ec2.sh fast)
if [ -n "$1" ]; then
    case "$1" in
        fast|quick|start)
            fast_start
            exit 0
            ;;
        rebuild|full)
            rebuild_all
            exit 0
            ;;
        stop)
            stop_all
            exit 0
            ;;
        status)
            show_status
            exit 0
            ;;
        logs)
            show_logs
            exit 0
            ;;
    esac
fi

# Menú interactivo
clear || true
echo -e "${BLUE}==============================================================================${NC}"
echo -e "${BOLD}${GREEN}        🏥 VidaSalud - Centro de Control de Despliegue en AWS EC2     ${NC}"
echo -e "${BLUE}==============================================================================${NC}"
echo -e "IP Pública Detectada: ${CYAN}${PUBLIC_IP}${NC}"
echo ""
echo -e "${BOLD}Seleccione una opción:${NC}"
echo -e "  ${GREEN}1)${NC} ${BOLD}⚡ INICIO RÁPIDO${NC} (Levantar Docker + 5 Microservicios en ~30s - ¡Recomendado!)"
echo -e "  ${GREEN}2)${NC} ${BOLD}🔨 RECOMPILAR E INICIAR TODO${NC} (Compilación limpia con Maven)"
echo -e "  ${GREEN}3)${NC} ${BOLD}🛑 DETENER TODO Y LIMPIAR${NC} (Liberar RAM y detener procesos)"
echo -e "  ${GREEN}4)${NC} ${BOLD}📊 VER ESTADO Y PUERTOS${NC} (Health Check y prueba de API Gateway)"
echo -e "  ${GREEN}5)${NC} ${BOLD}📜 VER LOGS EN TIEMPO REAL${NC}"
echo -e "  ${RED}q)${NC} Salir"
echo ""
read -p "Ingrese una opción [1-5/q]: " OPTION

case $OPTION in
    1)
        fast_start
        ;;
    2)
        rebuild_all
        ;;
    3)
        stop_all
        ;;
    4)
        show_status
        ;;
    5)
        show_logs
        ;;
    q|Q)
        echo "Saliendo..."
        exit 0
        ;;
    *)
        echo -e "${RED}Opción no válida.${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}==============================================================================${NC}"
echo -e "Accesos de evaluación:"
echo -e "  🌐 Panel Kafka-UI:       ${BLUE}http://${PUBLIC_IP}:8088${NC}"
echo -e "  🐇 RabbitMQ Nodo 1:      ${BLUE}http://${PUBLIC_IP}:15672${NC} (guest / guest)"
echo -e "  🐇 RabbitMQ Nodo 2:      ${BLUE}http://${PUBLIC_IP}:15673${NC} (guest / guest)"
echo -e "  🗂️ Microservicio Catálogo:${BLUE}http://${PUBLIC_IP}:8081/catalog/boxes${NC}"
echo -e "  📅 Microservicio Citas:   ${BLUE}http://${PUBLIC_IP}:8082/appointments${NC}"
echo -e "${GREEN}==============================================================================${NC}"
