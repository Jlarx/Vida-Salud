<div align="center">
  
# 🏥 VidaSalud - Sistema de Gestión Médica
  
**Arquitectura de Microservicios | Zero Trust Security | Frontend Reactivo**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen.svg?logo=spring)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.2.0-blue.svg?logo=react)](https://reactjs.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg?logo=postgresql)](https://www.postgresql.org/)
[![Kafka](https://img.shields.io/badge/Apache_Kafka-Event--Driven-E23237.svg?logo=apachekafka)](https://kafka.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED.svg?logo=docker)](https://www.docker.com/)
[![AWS EC2](https://img.shields.io/badge/AWS-EC2-FF9900.svg?logo=amazon-aws)](https://aws.amazon.com/ec2/)
[![Azure Entra ID](https://img.shields.io/badge/Azure_Entra_ID-MSAL-0078D4.svg?logo=microsoft-azure)](https://azure.microsoft.com/)

</div>

---

## 📖 Descripción del Proyecto
**VidaSalud** es una plataforma de gestión médica de nivel empresarial diseñada para manejar reservas de citas, administración de boxes y auditorías clínicas. El proyecto adopta una arquitectura basada en **Microservicios** en AWS, mensajería asíncrona con **Apache Kafka** y seguridad **Zero Trust**, centralizando la validación de identidad a través de Microsoft Azure Entra ID.

## 🏗️ Arquitectura del Sistema en AWS

El repositorio centraliza tanto el frontend como el backend, segmentados de la siguiente forma:

### 🖥️ Frontend
* **`/frontend`**: Interfaz de usuario construida con **React (Vite)**.
  * Autenticación segura y fluida usando `@azure/msal-react` (Flujo OAuth2.0 con Azure).
  * Renderizado condicional dinámico basado en los roles extraídos del JWT (Claims).

### 🛡️ API Gateway y Red AWS
* La arquitectura expone sus servicios de forma segura a internet mediante un **API Gateway** proxy.
* El frontend de React realiza las peticiones hacia el API Gateway.
* El API Gateway balancea y redirige el tráfico internamente hacia nuestra instancia de **Amazon EC2**, donde viven los microservicios, manteniéndolos aislados del internet público directo.

### ⚙️ Microservicios en Amazon EC2 (Spring Boot)
Los servicios de backend operan en una instancia EC2 bajo el patrón *Database-per-service* de PostgreSQL y se comunican asíncronamente mediante *Apache Kafka*.

| Servicio | Función Principal | Seguridad |
|----------|-------------------|-----------|
| 📅 **`/ms-vidasalud-appointments`** | Agenda y control de estados de atenciones. **Actúa como Productor Kafka** | `CLIENTE`, `OPERADOR`, `ADMINISTRADOR` |
| 🗂️ **`/ms-vidasalud-catalog`** | Gestión de boxes médicos y prestaciones. | `ADMINISTRADOR` (Escritura) |
| 🔒 **`/ms-vidasalud-audit`** | Trazabilidad inmutable de eventos de citas. **Consumidor Kafka** | `AUDITOR`, `ADMINISTRADOR` |
| 📊 **`/ms-vidasalud-report`** | Generación de estadísticas | *En desarrollo* |
| 🔔 **`/ms-vidasalud-notify`** | Envío de correos y alertas | *En desarrollo* |

---

## 🚀 Guía de Instalación Rápida

La infraestructura local está orquestada con **Docker Compose** para garantizar que las bases de datos independientes se desplieguen automáticamente.

### Prerrequisitos
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) ejecutándose en tu máquina.
- Java 21+ y Node.js (v18+).

### Ejecución en 1 Clic (Windows)
1. Clona este repositorio:
   ```bash
   git clone https://github.com/Jlarx/Vida-Salud.git
   ```
2. Ejecuta el archivo orquestador ubicado en la carpeta `/infra`:
   ```cmd
   .\infra\start-all.bat
   ```
3. **¿Qué sucede al ejecutarlo?**
   * Docker levantará 5 bases de datos de PostgreSQL independientes en el puerto `5432` y un clúster de **Apache Kafka**.
   * Se compilarán e iniciarán los microservicios de Spring Boot.
   * Se abrirá automáticamente la aplicación web React en `http://localhost:5173`.

### Despliegue en AWS EC2 (Producción)
Para aplicar actualizaciones en la nube:
```bash
# 1. Actualizar código
git pull

# 2. Reiniciar Microservicios
sudo pkill -f java
cd ~/Vida-Salud/ms-vidasalud-appointments && mvn clean package -DskipTests && nohup ./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx200m" > appt.log 2>&1 &
cd ~/Vida-Salud/ms-vidasalud-audit && mvn clean package -DskipTests && nohup ./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx200m" > audit.log 2>&1 &
cd ~/Vida-Salud/ms-vidasalud-catalog && mvn clean package -DskipTests && nohup ./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx200m" > catalog.log 2>&1 &
```

---

## 🔐 Seguridad y Autenticación
Este sistema implementa el modelo **Zero Trust**. 
1. El usuario inicia sesión mediante **Microsoft Azure Entra ID**.
2. React adjunta el `Bearer Token` en cada petición web dirigida al AWS API Gateway.
3. El **API Gateway** redirige el tráfico al microservicio correspondiente.
4. Cada microservicio de **Spring Boot** valida criptográficamente el JWT con Azure y aplica las reglas de autorización (ej. `ADMIN`, `OPERADOR`, `CLIENTE`).

<br/>
<div align="center">
  <i>Construido con pasión para el futuro de la gestión hospitalaria inteligente.</i>
</div>