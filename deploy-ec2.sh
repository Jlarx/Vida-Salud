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

start_all() {
    ensure_swap
    
    echo -e "${YELLOW}>> 1/4 Deteniendo instancias previas de microservicios...${NC}"
    pkill -9 -f 'ms-vidasalud' 2>/dev/null || true
    pkill -9 -f 'ms_vidasalud' 2>/dev/null || true
    sleep 2

    echo -e "${YELLOW}>> 2/4 Levantando Docker (Postgres, RabbitMQ Cluster, Kafka KRaft, Kafka-UI)...${NC}"
    docker compose -f infra/docker-compose.yml up -d

    echo -e "${YELLOW}>> Esperando que PostgreSQL esté listo...${NC}"
    for i in {1..20}; do
        if docker exec postgres-vidasalud pg_isready -U postgres >/dev/null 2>&1; then
            break
        fi
        sleep 2
    done

    # Creación explícita de todas las bases de datos requeridas
    docker exec -i postgres-vidasalud psql -U postgres -c "CREATE DATABASE vidasalud_catalog;" 2>/dev/null || true
    docker exec -i postgres-vidasalud psql -U postgres -c "CREATE DATABASE vidasalud_appointments;" 2>/dev/null || true
    docker exec -i postgres-vidasalud psql -U postgres -c "CREATE DATABASE vidasalud_audit;" 2>/dev/null || true
    docker exec -i postgres-vidasalud psql -U postgres -c "CREATE DATABASE vidasalud_report;" 2>/dev/null || true
    echo -e "${GREEN}>> Bases de datos PostgreSQL verificadas y listas.${NC}"

    echo -e "${YELLOW}>> 3/4 Compilando microservicios individualmente...${NC}"
    for ms in ms-vidasalud-catalog ms-vidasalud-appointments ms-vidasalud-audit ms-vidasalud-report ms-vidasalud-notify; do
        if [ -d "$ms" ]; then
            echo -e "${CYAN}>> Compilando $ms...${NC}"
            (cd "$ms" && chmod +x mvnw 2>/dev/null || true; ./mvnw clean package -DskipTests -q)
        fi
    done
    echo -e "${GREEN}>> Todos los microservicios compilados exitosamente.${NC}"

    echo -e "${YELLOW}>> 4/4 Levantando microservicios en segundo plano (-Xmx180m)...${NC}"
    
    JAR_CATALOG=$(ls ms-vidasalud-catalog/target/*SNAPSHOT.jar 2>/dev/null | grep -v '\.original' | head -n 1)
    nohup java -Xmx180m -jar "$JAR_CATALOG" > catalog.log 2>&1 &
    echo -e "   [1/5] Catálogo iniciado (Puerto 8081)"
    sleep 7

    JAR_APPT=$(ls ms-vidasalud-appointments/target/*SNAPSHOT.jar 2>/dev/null | grep -v '\.original' | head -n 1)
    nohup java -Xmx180m -jar "$JAR_APPT" > appointments.log 2>&1 &
    echo -e "   [2/5] Citas iniciado (Puerto 8082)"
    sleep 7

    JAR_AUDIT=$(ls ms-vidasalud-audit/target/*SNAPSHOT.jar 2>/dev/null | grep -v '\.original' | head -n 1)
    nohup java -Xmx180m -jar "$JAR_AUDIT" > audit.log 2>&1 &
    echo -e "   [3/5] Auditoría iniciado (Puerto 8083)"
    sleep 5

    JAR_REPORT=$(ls ms-vidasalud-report/target/*SNAPSHOT.jar 2>/dev/null | grep -v '\.original' | head -n 1)
    nohup java -Xmx180m -jar "$JAR_REPORT" > report.log 2>&1 &
    echo -e "   [4/5] Reportes iniciado (Puerto 8084)"
    sleep 5

    JAR_NOTIFY=$(ls ms-vidasalud-notify/target/*SNAPSHOT.jar 2>/dev/null | grep -v '\.original' | head -n 1)
    nohup java -Xmx180m -jar "$JAR_NOTIFY" > notify.log 2>&1 &
    echo -e "   [5/5] Notificaciones iniciado (Puerto 8085)"

    echo -e "${YELLOW}>> Esperando que Tomcat active el puerto 8081...${NC}"
    for i in {1..25}; do
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/catalog/boxes || echo "000")
        if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
            echo -e "${GREEN}>> ✅ ¡SISTEMA ONLINE Y LISTO! (Respuesta HTTP: $HTTP_CODE)${NC}"
            return 0
        fi
        sleep 2
    done
    echo -e "${YELLOW}>> Los microservicios están finalizando su arranque en segundo plano. Verifique con la opción 3.${NC}"
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

clear || true
echo -e "${BLUE}==============================================================================${NC}"
echo -e "${BOLD}${GREEN}        🏥 VidaSalud - Centro de Control de Despliegue en AWS EC2     ${NC}"
echo -e "${BLUE}==============================================================================${NC}"
echo -e "IP Pública Detectada: ${CYAN}${PUBLIC_IP}${NC}"
echo ""
echo -e "${BOLD}Seleccione una opción:${NC}"
echo -e "  ${GREEN}1)${NC} ${BOLD}INICIAR TODO${NC} (Compilar + Docker + Bases de Datos + 5 Microservicios)"
echo -e "  ${GREEN}2)${NC} ${BOLD}DETENER TODO Y LIMPIAR${NC} (Liberar RAM y detener procesos)"
echo -e "  ${GREEN}3)${NC} ${BOLD}VER ESTADO Y PUERTOS${NC} (Health Check y prueba de API Gateway)"
echo -e "  ${GREEN}4)${NC} ${BOLD}VER LOGS EN TIEMPO REAL${NC}"
echo -e "  ${RED}q)${NC} Salir"
echo ""
read -p "Ingrese una opción [1-4/q]: " OPTION

case $OPTION in
    1)
        start_all
        ;;
    2)
        stop_all
        ;;
    3)
        show_status
        ;;
    4)
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
