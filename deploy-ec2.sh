#!/bin/bash
# ==============================================================================
# Script de Despliegue Automatizado en AWS EC2 - VidaSalud
# ==============================================================================

set -e

# Colores para salida limpia
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}==============================================================================${NC}"
echo -e "${GREEN}      🏥 VidaSalud - Despliegue Automatizado en AWS EC2      ${NC}"
echo -e "${BLUE}==============================================================================${NC}"

# 1. Obtener IP pública de la EC2
PUBLIC_IP=$(curl -s --connect-timeout 2 http://checkip.amazonaws.com || curl -s --connect-timeout 2 http://169.254.169.254/latest/meta-data/public-ipv4 || echo "TU_IP_EC2")
echo -e "${YELLOW}>> IP Pública detectada: ${PUBLIC_IP}${NC}"

# 2. Verificar y activar memoria SWAP (Crucial en t2/t3.micro de 1GB)
SWAP_TOTAL=$(free -m | awk '/Swap:/ {print $2}')
if [ "$SWAP_TOTAL" -lt 2000 ]; then
    echo -e "${YELLOW}>> Swap insuficiente ($SWAP_TOTAL MB). Configurando Swap de 4GB para evitar bloqueos...${NC}"
    sudo fallocate -l 4G /swapfile || sudo dd if=/dev/zero of=/swapfile bs=1M count=4096
    sudo chmod 600 /swapfile
    sudo mkswap /swapfile
    sudo swapon /swapfile || true
    echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab > /dev/null
    echo -e "${GREEN}>> Memoria Swap de 4GB activada correctamente.${NC}"
else
    echo -e "${GREEN}>> Memoria Swap OK ($SWAP_TOTAL MB detectados).${NC}"
fi

# 3. Menú de Opciones
echo ""
echo -e "${BLUE}Seleccione una opción:${NC}"
echo "1) Levantar TODO (PostgreSQL + RabbitMQ Clúster + Kafka Clúster + Backend)"
echo "2) Levantar sólo RabbitMQ Clúster (2 nodos) + PostgreSQL"
echo "3) Levantar sólo Kafka Clúster (3 brokers KRaft + Kafka-UI en puerto 8088)"
echo "4) Compilar y Levantar los 5 Microservicios Backend en segundo plano"
echo "5) Detener todos los microservicios Java"
echo "6) Ver estado de contenedores y procesos Java"
echo "q) Salir"
echo ""
read -p "Ingrese su opción [1-6/q]: " OPTION

case $OPTION in
    1)
        echo -e "${YELLOW}>> Levantando contenedores unificados (PostgreSQL, RabbitMQ 2 Nodos, Kafka 3 Brokers)...${NC}"
        docker compose -f infra/docker-compose.yml up -d
        echo -e "${GREEN}>> Contenedores iniciados. Esperando 10 segundos para estabilización...${NC}"
        sleep 10
        
        echo -e "${YELLOW}>> Deteniendo procesos Java previos si existen...${NC}"
        sudo pkill -f java || true
        
        echo -e "${YELLOW}>> Compilando microservicios individualmente...${NC}"
        cd ms-vidasalud-catalog && ./mvnw clean package -DskipTests && cd ..
        cd ms-vidasalud-appointments && ./mvnw clean package -DskipTests && cd ..
        cd ms-vidasalud-audit && ./mvnw clean package -DskipTests && cd ..
        cd ms-vidasalud-report && ./mvnw clean package -DskipTests && cd ..
        cd ms-vidasalud-notify && ./mvnw clean package -DskipTests && cd ..

        echo -e "${YELLOW}>> Levantando microservicios en segundo plano (-Xmx180m)...${NC}"
        nohup java -Xmx180m -jar ms-vidasalud-catalog/target/*.jar > catalog.log 2>&1 &
        sleep 4
        nohup java -Xmx180m -jar ms-vidasalud-appointments/target/*.jar > appointments.log 2>&1 &
        sleep 4
        nohup java -Xmx180m -jar ms-vidasalud-audit/target/*.jar > audit.log 2>&1 &
        sleep 4
        nohup java -Xmx180m -jar ms-vidasalud-report/target/*.jar > report.log 2>&1 &
        sleep 4
        nohup java -Xmx180m -jar ms-vidasalud-notify/target/*.jar > notify.log 2>&1 &
        sleep 3
        ;;

    2)
        echo -e "${YELLOW}>> Levantando PostgreSQL y Clúster RabbitMQ de 2 nodos...${NC}"
        docker compose -f infra/docker-compose-rabbitmq-cluster.yml up -d
        ;;

    3)
        echo -e "${YELLOW}>> Levantando Clúster Kafka (3 brokers KRaft) y Kafka-UI...${NC}"
        docker compose -f infra/docker-compose-kafka.yml up -d
        ;;

    4)
        echo -e "${YELLOW}>> Deteniendo procesos Java previos...${NC}"
        sudo pkill -f java || true

        echo -e "${YELLOW}>> Compilando microservicios...${NC}"
        cd ms-vidasalud-catalog && ./mvnw clean package -DskipTests && cd ..
        cd ms-vidasalud-appointments && ./mvnw clean package -DskipTests && cd ..
        cd ms-vidasalud-audit && ./mvnw clean package -DskipTests && cd ..
        cd ms-vidasalud-report && ./mvnw clean package -DskipTests && cd ..
        cd ms-vidasalud-notify && ./mvnw clean package -DskipTests && cd ..

        echo -e "${YELLOW}>> Iniciando microservicios (-Xmx180m)...${NC}"
        nohup java -Xmx180m -jar ms-vidasalud-catalog/target/*.jar > catalog.log 2>&1 &
        sleep 4
        nohup java -Xmx180m -jar ms-vidasalud-appointments/target/*.jar > appointments.log 2>&1 &
        sleep 4
        nohup java -Xmx180m -jar ms-vidasalud-audit/target/*.jar > audit.log 2>&1 &
        sleep 4
        nohup java -Xmx180m -jar ms-vidasalud-report/target/*.jar > report.log 2>&1 &
        sleep 4
        nohup java -Xmx180m -jar ms-vidasalud-notify/target/*.jar > notify.log 2>&1 &
        sleep 3
        ;;

    5)
        echo -e "${YELLOW}>> Deteniendo todos los procesos Java...${NC}"
        sudo pkill -f java || true
        echo -e "${GREEN}>> Procesos Java detenidos.${NC}"
        exit 0
        ;;

    6)
        echo -e "${BLUE}--- ESTADO DE DOCKER ---${NC}"
        docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
        echo ""
        echo -e "${BLUE}--- PROCESOS JAVA EN EJECUCION ---${NC}"
        ps aux | grep java | grep -v grep || echo "No hay procesos Java activos."
        exit 0
        ;;

    q|Q)
        echo "Operación cancelada."
        exit 0
        ;;

    *)
        echo -e "${RED}Opción inválida.${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}==============================================================================${NC}"
echo -e "${GREEN}               ✅ DESPLIEGUE FINALIZADO EXITOSAMENTE                          ${NC}"
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

