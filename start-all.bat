@echo off
echo ==============================================
echo   Iniciando Infraestructura y Microservicios
echo ==============================================

docker compose -f infra/docker-compose.yml up -d
echo Esperando inicializacion de PostgreSQL y RabbitMQ...
timeout /t 8 /nobreak >nul

start "Catalog (Port 8081)" cmd /k "cd ms-vidasalud-catalog && .\mvnw.cmd spring-boot:run"
start "Appointments (Port 8082)" cmd /k "cd ms-vidasalud-appointments && .\mvnw.cmd spring-boot:run"
start "Audit (Port 8083)" cmd /k "cd ms-vidasalud-audit && .\mvnw.cmd spring-boot:run"
start "Report (Port 8084)" cmd /k "cd ms-vidasalud-report && .\mvnw.cmd spring-boot:run"
start "Notify (Port 8085)" cmd /k "cd ms-vidasalud-notify && .\mvnw.cmd spring-boot:run"

timeout /t 5 /nobreak >nul
start "Frontend (Vite)" cmd /k "cd frontend && npm run dev"

echo Todos los servicios han sido lanzados en ventanas separadas.

