@echo off
docker compose up -d
timeout /t 10 /nobreak >nul

start "Catalog" cmd /k "cd ms-vidasalud-catalog && .\mvnw.cmd spring-boot:run"
start "Appointments" cmd /k "cd ms-vidasalud-appointments && .\mvnw.cmd spring-boot:run"
start "Audit" cmd /k "cd ms-vidasalud-audit && .\mvnw.cmd spring-boot:run"
start "Report" cmd /k "cd ms-vidasalud-report && .\mvnw.cmd spring-boot:run"
start "Notify" cmd /k "cd ms-vidasalud-notify && .\mvnw.cmd spring-boot:run"

timeout /t 5 /nobreak >nul
start "BFF Gateway" cmd /k "cd ms-vidasalud-bff && .\mvnw.cmd spring-boot:run"

timeout /t 5 /nobreak >nul
start "Frontend" cmd /k "cd frontend && npm run dev"

:: .\start-all.bat para correrlo en la terminal

