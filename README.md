<div align="center">
  
# 🏥 VidaSalud - Sistema de Gestión Médica
  
**Arquitectura de Microservicios | Zero Trust Security | Mensajería Asíncrona | Frontend Reactivo**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.8-brightgreen.svg?logo=spring)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-blue.svg?logo=react)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-6-646CFF.svg?logo=vite)](https://vitejs.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg?logo=postgresql)](https://www.postgresql.org/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-AMQP-FF6600.svg?logo=rabbitmq)](https://www.rabbitmq.com/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED.svg?logo=docker)](https://www.docker.com/)
[![AWS EC2](https://img.shields.io/badge/AWS-EC2-FF9900.svg?logo=amazon-aws)](https://aws.amazon.com/ec2/)
[![AWS API Gateway](https://img.shields.io/badge/AWS-API_Gateway-FF4F8B.svg?logo=amazon-aws)](https://aws.amazon.com/api-gateway/)
[![Azure Entra ID](https://img.shields.io/badge/Azure_Entra_ID-MSAL-0078D4.svg?logo=microsoft-azure)](https://azure.microsoft.com/)

</div>

---

## 📖 Descripción del Proyecto
**VidaSalud** es una plataforma de gestión médica de nivel empresarial diseñada para manejar reservas de citas, administración de boxes y auditorías clínicas. El proyecto adopta una arquitectura basada en **Microservicios** en AWS, mensajería asíncrona tolerante a fallos con **RabbitMQ (AMQP / Dead Letter Queues)** y seguridad **Zero Trust**, centralizando la validación de identidad a través de Microsoft Azure Entra ID.

---

## 🏗️ Arquitectura del Sistema

```
                      +---------------------------------------+
                      |       Frontend Web (React + Vite)     |
                      |  - Autenticación OAuth2 / Azure MSAL  |
                      +-------------------+-------------------+
                                          | HTTPS (Bearer Token)
                                          v
                      +---------------------------------------+
                      |         AWS API Gateway               |
                      +-------------------+-------------------+
                                          | Enrutamiento HTTP
                                          v
      +-----------------------------------------------------------------------+
      |                  Instancia AWS EC2 (Microservicios)                   |
      |                                                                       |
      |  +---------------------------+       +-----------------------------+  |
      |  | ms-vidasalud-appointments | ----> |          RabbitMQ           |  |
      |  |        (Puerto 8082)      | Event |  - Exchanges y Colas        |  |
      |  +-------------+-------------+       |  - Dead Letter Queue (DLQ)  |  |
      |                |                     +--------------+--------------+  |
      |  +-------------+-------------+                      |                 |
      |  |    ms-vidasalud-catalog   |                      | Eventos         |
      |  |        (Puerto 8081)      |                      v                 |
      |  +-------------+-------------+       +--------------+--------------+  |
      |                |                     |      ms-vidasalud-audit     |  |
      |  +-------------+-------------+       |        (Puerto 8083)        |  |
      |  |     ms-vidasalud-report   |       +--------------+--------------+  |
      |  |        (Puerto 8084)      |                      |                 |
      |  +-------------+-------------+       +--------------+--------------+  |
      |                                      |      ms-vidasalud-notify    |  |
      |                                      |        (Puerto 8085)        |  |
      |                                      +-----------------------------+  |
      |                                                                       |
      |  +-----------------------------------------------------------------+  |
      |  |                 PostgreSQL 15 (Docker Multi-DB)                 |  |
      |  |  vidasalud_appointments | vidasalud_catalog | vidasalud_audit  |  |
      |  |  vidasalud_report       | vidasalud         (Puerto 5432)       |  |
      |  +-----------------------------------------------------------------+  |
      +-----------------------------------------------------------------------+
```

### 🖥️ Frontend
* **`/frontend`**: Interfaz de usuario construida con **React y Vite**.
  * Autenticación segura usando `@azure/msal-react` y `@azure/msal-browser` (Flujo OAuth2.0 / OpenID Connect con Azure Entra ID).
  * Renderizado condicional dinámico según los roles/claims del JWT.

### 🛡️ API Gateway y Red AWS
* La arquitectura expone sus endpoints de forma centralizada mediante **AWS API Gateway**.
* El frontend realiza sus solicitudes al API Gateway (`/api/...`), el cual enruta el tráfico directamente hacia la instancia **Amazon EC2**.
* Cada microservicio valida criptográficamente la firma del JWT con los servidores de Azure Entra ID antes de procesar cualquier solicitud.

### ⚙️ Microservicios de Backend (Spring Boot 4 / Java 21)
Operan bajo el patrón *Database-per-service* de PostgreSQL y mensajería con *RabbitMQ*:

| Servicio | Puerto | Función Principal | Seguridad |
|---|---|---|---|
| 📅 **`/ms-vidasalud-appointments`** | `8082` | Gestión y control de citas médicas. Publicador de eventos AMQP. | `CLIENTE`, `OPERADOR`, `ADMINISTRADOR` |
| 🗂️ **`/ms-vidasalud-catalog`** | `8081` | Gestión de boxes médicos y catálogo de prestaciones. | `ADMINISTRADOR` (Escritura) |
| 🔒 **`/ms-vidasalud-audit`** | `8083` | Trazabilidad inmutable de cambios de estado (Consumidor AMQP con ACK manual y DLQ). | `AUDITOR`, `ADMINISTRADOR` |
| 📊 **`/ms-vidasalud-report`** | `8084` | Generación de estadísticas y reportería clínica. | `ADMINISTRADOR`, `AUDITOR` |
| 🔔 **`/ms-vidasalud-notify`** | `8085` | Consumidor de eventos para envío de notificaciones y alertas. | Uso interno / AMQP |

---

## 🚀 Guía de Instalación y Ejecución Local

### Prerrequisitos
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) ejecutándose.
- Java 21 LTS instalado.
- Node.js (v18+) y npm.

### Ejecución en 1 Clic (Windows)
1. Clona el repositorio:
   ```bash
   git clone https://github.com/Jlarx/Vida-Salud.git
   cd Vida-Salud
   ```
2. Ejecuta el archivo orquestador:
   ```cmd
   .\start-all.bat
   ```
3. **¿Qué realiza `start-all.bat`?**
   * Levanta los contenedores de Docker (`PostgreSQL 15` multi-base de datos y `RabbitMQ` con panel de control en `http://localhost:15672`).
   * Inicia cada microservicio de Spring Boot en ventanas independientes.
   * Inicia el servidor de desarrollo del Frontend en `http://localhost:5173`.

---

## ☁️ Despliegue en AWS EC2 (Paso a Paso sin cuelgues)

> [!IMPORTANT]
> En instancias EC2 con recursos ajustados (como `t2.micro` o `t3.micro` con 1 GB de RAM), es indispensable configurar **Swap** y compilar los microservicios individualmente para evitar que la instancia se congele por falta de memoria.

### 1. Configurar Swap (4 GB) en la EC2 (Solo se hace una vez)
```bash
sudo fallocate -l 4G /swapfile || sudo dd if=/dev/zero of=/swapfile bs=1M count=4096
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

### 2. Clonar o Actualizar el Repositorio
```bash
cd ~
# Si clonas por primera vez:
git clone https://github.com/Jlarx/Vida-Salud.git
cd Vida-Salud

# Si ya estaba clonado:
cd ~/Vida-Salud
git pull origin main
```

### 3. Levantar la Infraestructura (Docker)
```bash
docker compose -f infra/docker-compose.yml down || true
docker compose -f infra/docker-compose.yml up -d
sleep 8
```

### 4. Compilar los Microservicios (Paso a Paso)
```bash
cd ~/Vida-Salud
sudo pkill -f java || true

cd ms-vidasalud-catalog && ./mvnw clean package -DskipTests && cd ..
cd ms-vidasalud-appointments && ./mvnw clean package -DskipTests && cd ..
cd ms-vidasalud-audit && ./mvnw clean package -DskipTests && cd ..
cd ms-vidasalud-report && ./mvnw clean package -DskipTests && cd ..
cd ms-vidasalud-notify && ./mvnw clean package -DskipTests && cd ..
```

### 5. Ejecutar en Segundo Plano con Límite de Memoria JVM
```bash
cd ~/Vida-Salud

nohup java -Xmx200m -jar ms-vidasalud-catalog/target/*.jar > catalog.log 2>&1 &
sleep 5

nohup java -Xmx200m -jar ms-vidasalud-appointments/target/*.jar > appointments.log 2>&1 &
sleep 5

nohup java -Xmx200m -jar ms-vidasalud-audit/target/*.jar > audit.log 2>&1 &
sleep 5

nohup java -Xmx200m -jar ms-vidasalud-report/target/*.jar > report.log 2>&1 &
sleep 5

nohup java -Xmx200m -jar ms-vidasalud-notify/target/*.jar > notify.log 2>&1 &
```

### 6. Monitoreo y Verificación
```bash
# Ver procesos Java activos:
ps aux | grep java

# Ver logs en vivo de cualquier servicio:
tail -f appointments.log
```

---

## 🔐 Seguridad y Autenticación (Zero Trust)
1. **Inicio de Sesión:** El usuario inicia sesión en el Frontend contra **Microsoft Azure Entra ID**.
2. **Token JWT:** El cliente adjunta el `Bearer Token` firmado en cada petición hacia el **AWS API Gateway**.
3. **Validación Criptográfica:** Cada microservicio valida independientemente el emisor (`issuer-uri`), la audiencia (`aud`) y los roles (`roles`/`scp`) del token mediante Spring Security y OAuth2 Resource Server.
4. **Control de Acceso Basado en Roles (RBAC):** Se aplican restricciones estrictas por perfil (`ADMINISTRADOR`, `OPERADOR`, `CLIENTE`, `AUDITOR`).

<br/>
<div align="center">
  <i>Construido con pasión para el futuro de la gestión hospitalaria inteligente.</i>
</div>
