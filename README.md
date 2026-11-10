<div align="center">
  
# 🏥 VidaSalud - Plataforma de Gestión Médica Cloud

**Arquitectura de Microservicios • Zero Trust Security • Streaming con Apache Kafka • Mensajería Clúster RabbitMQ**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.8-brightgreen.svg?logo=spring)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-blue.svg?logo=react)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-6-646CFF.svg?logo=vite)](https://vitejs.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg?logo=postgresql)](https://www.postgresql.org/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ_Clúster-2_Nodos-FF6600.svg?logo=rabbitmq)](https://www.rabbitmq.com/)
[![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-3_Brokers_KRaft-231F20.svg?logo=apachekafka)](https://kafka.apache.org/)
[![Kafka-UI](https://img.shields.io/badge/Kafka--UI-Puerto_8088-00C7B7.svg)](http://52.1.200.223:8088)
[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED.svg?logo=docker)](https://www.docker.com/)
[![AWS EC2](https://img.shields.io/badge/AWS-EC2_Ubuntu-FF9900.svg?logo=amazon-aws)](https://aws.amazon.com/ec2/)
[![AWS API Gateway](https://img.shields.io/badge/AWS-API_Gateway-FF4F8B.svg?logo=amazon-aws)](https://aws.amazon.com/api-gateway/)
[![Azure Entra ID](https://img.shields.io/badge/Azure_Entra_ID-OAuth2_MSAL-0078D4.svg?logo=microsoft-azure)](https://azure.microsoft.com/)

---

### 🌐 Accesos Directos de Evaluación en Vivo (AWS Cloud)
| Servicio | Enlace de Demostración | Credenciales / Tipo |
|---|---|---|
| 📊 **Kafka-UI (3 Brokers / 2 Tópicos / 3 Réplicas)** | [http://52.1.200.223:8088](http://52.1.200.223:8088) | Acceso Libre |
| 🐇 **RabbitMQ Clúster - Nodo 1** | [http://52.1.200.223:15672](http://52.1.200.223:15672) | `guest` / `guest` |
| 🐇 **RabbitMQ Clúster - Nodo 2** | [http://52.1.200.223:15673](http://52.1.200.223:15673) | `guest` / `guest` |
| 🌐 **AWS API Gateway (Producción)** | `https://zzyt3obhvf.execute-api.us-east-1.amazonaws.com/prod` | Bearer JWT (Azure Entra ID) |

---

</div>

## 📖 Descripción del Proyecto
**VidaSalud** es una solución integral de gestión clínica diseñada bajo los estándares de la industria en **arquitecturas distribuidas, seguridad Zero Trust y tolerancia a fallos**. Permite administrar atenciones médicas, controlar la capacidad física de boxes, auditar de manera inmutable el ciclo de vida de los pacientes y emitir alertas operativas y estadísticas en tiempo real.

---

## 🏗️ Arquitectura Integral del Sistema

```
                        +-----------------------------------------------+
                        |       Frontend Reactivo (React 19 + Vite)     |
                        |      - Autenticación OAuth 2.0 / Azure MSAL   |
                        +-----------------------+-----------------------+
                                                | HTTPS + Bearer JWT
                                                v
                        +-----------------------------------------------+
                        |       AWS API Gateway (Edge Routing)          |
                        | - Valida firmas JWT y enruta a la instancia   |
                        +-----------------------+-----------------------+
                                                | HTTP Interno
                                                v
    +-----------------------------------------------------------------------------------+
    |                        Instancia Amazon EC2 (Ubuntu Cloud)                        |
    |                                                                                   |
    |   +---------------------------------------+   +-------------------------------+   |
    |   |     Microservicios Spring Boot 4      |   |   Clúster RabbitMQ (2 Nodos)  |   |
    |   |                                       |   | - rabbitmq-node1 (Port 5672)  |   |
    |   | • ms-catalog      (Puerto 8081)       |   | - rabbitmq-node2 (Port 5673)  |   |
    |   | • ms-appointments (Puerto 8082) ----> |   |                               |   |
    |   | • ms-audit        (Puerto 8083) <---- |   | • Exchanges: DIRECT y TOPIC   |   |
    |   | • ms-report       (Puerto 8084) <---- |   | • 3 Colas: audit, notify,     |   |
    |   | • ms-notify       (Puerto 8085) <---- |   |   direct + 3 DLQ dedicadas    |   |
    |   +---------------------------------------+   +-------------------------------+   |
    |                       |                                                           |
    |                       | Eventos Streaming     +-------------------------------+   |
    |                       +---------------------> |  Clúster Apache Kafka (KRaft) |   |
    |                       |                       | - kafka-1 (9092)              |   |
    |                       v                       | - kafka-2 (9094)              |   |
    |   +---------------------------------------+   | - kafka-3 (9096)              |   |
    |   |   PostgreSQL 15 (Docker Multi-DB)     |   | • Tópicos: vidasalud-citas,   |   |
    |   | • vidasalud_appointments              |   |   vidasalud-alertas           |   |
    |   | • vidasalud_catalog                   |   |   (3 Particiones, 3 Réplicas) |   |
    |   | • vidasalud_audit                     |   | • Kafka-UI: Puerto 8088       |   |
    |   | • vidasalud_report                    |   +-------------------------------+   |
    |   +---------------------------------------+                                       |
    +-----------------------------------------------------------------------------------+
```

---

## ⚙️ Matriz de Microservicios Backend

Cada microservicio opera con su propia base de datos (*Database-per-service*) y control de acceso basado en roles (**RBAC**):

| Microservicio | Puerto | Función Principal | Seguridad y Roles |
|---|---|---|---|
| 📅 **`ms-vidasalud-appointments`** | `8082` | Gestión y control del ciclo de vida de citas médicas. Publicador AMQP en Exchanges Topic y Direct. | `CLIENTE`, `OPERADOR`, `ADMINISTRADOR` |
| 🗂️ **`ms-vidasalud-catalog`** | `8081` | Administración de consultorios (Boxes) y catálogo de prestaciones médicas. | `ADMINISTRADOR` (Escritura), Autenticado (Lectura) |
| 🔒 **`ms-vidasalud-audit`** | `8083` | Trazabilidad inmutable de estados con confirmación manual (**Manual ACK**) y reenvío a DLQ ante fallos. | `AUDITOR`, `ADMINISTRADOR` |
| 📊 **`ms-vidasalud-report`** | `8084` | Estadísticas y reportería clínica. Consumidor directo de eventos mediante **DirectExchange**. | `ADMINISTRADOR`, `AUDITOR` |
| 🔔 **`ms-vidasalud-notify`** | `8085` | Consumidor AMQP de alertas y notificaciones con tolerancia a fallos (DLQ). | Uso interno / AMQP |

---

## 📨 Topología de Mensajería y Streaming

### 🐇 1. Clúster de RabbitMQ (Alta Disponibilidad)
* **2 Nodos en Clúster:** `rabbitmq-node1` y `rabbitmq-node2` sincronizados mediante auto-clustering nativo.
* **Exchanges Configurados:**
  * `direct.exchange` (**DirectExchange**): Enrutamiento puntual mediante clave exacta (`direct.routing.key`).
  * `audit.exchange` (**TopicExchange**): Enrutamiento por tópicos de auditoría (`audit.routing.key`).
  * `notify.exchange` (**TopicExchange**): Enrutamiento de notificaciones operativas (`notify.routing.key`).
  * `deadLetterExchange` (**TopicExchange / DLX**): Enrutador centralizado de mensajes rechazados o erróneos hacia las DLQs.
* **3 Colas de Mensajes y sus 3 DLQs Activas:**
  1. `audit.queue` $\leftrightarrow$ DLQ: `audit.dlq`
  2. `notify.queue` $\leftrightarrow$ DLQ: `notify.dlq`
  3. `direct.queue` $\leftrightarrow$ DLQ: `direct.dlq`

### ⚡ 2. Clúster Apache Kafka (KRaft Mode) y Kafka-UI
* **3 Brokers en Quórum KRaft:** `kafka-1`, `kafka-2` y `kafka-3` operando sin dependencia de Zookeeper.
* **Panel de Control Kafka-UI:** Accesible vía web en el puerto **`8088`**.
* **Tópicos Creados:**
  * `vidasalud-citas`: **3 particiones** y **factor de replicación 3**.
  * `vidasalud-alertas`: **3 particiones** y **factor de replicación 3**.

---

## 🚀 Despliegue en AWS EC2 (Paso a Paso Automatizado)

Para garantizar un inicio fluido y sin saturación de memoria en instancias con 1 GB de RAM, se incluye el script orquestador [`deploy-ec2.sh`](./deploy-ec2.sh):

```bash
# 1. Conéctate a tu instancia EC2 por SSH:
ssh -i "tu-llave.pem" ubuntu@52.1.200.223

# 2. Clona o actualiza el repositorio:
cd ~/Vida-Salud
git pull origin main

# 3. Ejecuta el script de despliegue automatizado:
chmod +x deploy-ec2.sh
./deploy-ec2.sh
```
*(Selecciona la **Opción 1** para levantar de forma secuencial la base de datos, los clústeres de mensajería y compilar/ejecutar los 5 microservicios con límites de memoria JVM optimizados).*

---

## 🔐 Seguridad Zero Trust y Gobernanza de Identidad (IDaaS)
1. **Identidad Federada:** Autenticación a través de **Microsoft Azure Entra ID** bajo el flujo OAuth 2.0 / OpenID Connect con PKCE.
2. **Validación Criptográfica:** Cada microservicio valida independientemente el `issuer-uri` de Microsoft, el ID de cliente de la aplicación y la firma RSA del JWT.
3. **Control de Acceso (RBAC):** Restricción estricta de rutas mediante claims y roles (`ADMINISTRADOR`, `OPERADOR`, `CLIENTE`, `AUDITOR`).

<br/>
<div align="center">
  <i>VidaSalud - Transformando la salud digital con arquitecturas cloud nativas y tolerantes a fallos.</i>
</div>
