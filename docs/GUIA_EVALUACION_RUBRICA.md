# 📋 Guía Maestra de Demostración y Defensa de Rúbrica - VidaSalud

Esta guía detalla de forma ordenada y concisa cómo demostrar ante el evaluador cada uno de los 12 puntos requeridos en la evaluación de arquitectura cloud, mensajería y seguridad.

---

## 🌐 1. Instancia de API Manager en Funcionamiento
* **Plataforma Cloud:** AWS API Gateway (o API Manager Cloud).
* **URL Pública del Gateway:** `https://zzyt3obhvf.execute-api.us-east-1.amazonaws.com/prod`
* **Demostración:**
  1. Abrir la consola de AWS -> Servicio **API Gateway**.
  2. Seleccionar el API HTTP/REST de `VidaSalud`.
  3. Mostrar el estado activo (*Deployed Stage: `prod`*).

---

## ⚙️ 2. Configuración de Rutas e Integración Backend
* **Demostración:**
  1. En la consola de API Gateway, ir a la sección **Routes / Resources**.
  2. Mostrar cómo las rutas (`/api/appointments/{proxy+}`, `/api/catalog/{proxy+}`, `/api/audit/{proxy+}`) tienen una **Integración HTTP (HTTP Proxy)** apuntando a la IP elástica de la EC2:
     - `http://52.1.200.223:8081/catalog` (Catálogo)
     - `http://52.1.200.223:8082/appointments` (Citas)
     - `http://52.1.200.223:8083/audit` (Auditoría)
     - `http://52.1.200.223:8084/report` (Reportería)

---

## 🖥️ 3. Frontend Consumiendo Endpoints vía API Manager
* **Demostración:**
  1. En el frontend, mostrar el archivo [`src/apiClient.js`](../frontend/src/apiClient.js):
     ```javascript
     const API_BASE_URL = import.meta.env.VITE_API_URL || 'https://zzyt3obhvf.execute-api.us-east-1.amazonaws.com/prod';
     ```
  2. En el navegador, abrir las Herramientas de Desarrollador (**F12 -> Pestaña Network / Red**).
  3. Al listar servicios o agendar una cita, mostrar que la petición HTTP se envía a `zzyt3obhvf.execute-api.us-east-1.amazonaws.com/prod/...` y recibe código `200 OK`.

---

## 🔒 4. Validación Criptográfica de JWT (Aceptación y Rechazo)
* **Demostración:**
  1. **Petición Válida:** Enviar una solicitud desde Postman o Frontend con el `Bearer Token` obtenido tras autenticarse:
     - Retorna: `200 OK`.
  2. **Petición Inválida / Rechazada:** Enviar una solicitud sin token o con token alterado:
     - URL: `https://zzyt3obhvf.execute-api.us-east-1.amazonaws.com/prod/catalog/boxes`
     - Header: `Authorization: Bearer token_falso_o_invalido`
     - Retorna: `401 Unauthorized` o `403 Forbidden` (demostrando que el sistema rechaza peticiones no autorizadas).

---

## 🆔 5. Tenant en IDaaS (Azure Entra ID) y Usuarios Registrados
* **Tenant ID:** `001834fd-b68a-4e6a-a718-6d859787b926`
* **App Client ID:** `09d40f2a-29ea-43c0-8d74-b89ba284c778`
* **Demostración:**
  1. Ingresar al portal de Microsoft Azure (`portal.azure.com`) -> **Microsoft Entra ID**.
  2. Mostrar la lista de **Usuarios (Users)** con sus roles asignados (Admin, Operador, Cliente, Auditor).
  3. Mostrar el **App Registration** de `VidaSalud` con los App Roles definidos (`ADMINISTRADOR`, `OPERADOR`, `CLIENTE`, `AUDITOR`).

---

## 🔑 6. Autenticación Frontend con OAuth 2.0 / OpenID Connect (MSAL)
* **Demostración:**
  1. Abrir la aplicación Frontend en el navegador.
  2. Pulsar **"Iniciar sesión con Microsoft Entra ID"**.
  3. Mostrar cómo la URL redirige a `login.microsoftonline.com/001834fd-b68a-4e6a-a718-6d859787b926/oauth2/v2.0/authorize` (flujo PKCE / OIDC).
  4. Ingresar credenciales y mostrar que al regresar, el token JWT recibido en sesión contiene claims de identidad y roles:
     - En consola del navegador: `sessionStorage` almacena las credenciales de MSAL.

---

## ☁️ 7. Despliegue en la Nube de Backend y Frontend Integrados
* **Instancia EC2:** IP Pública `52.1.200.223`
* **Demostración:**
  1. Conectarse a la EC2 por SSH:
     ```bash
     ssh -i "tu-llave.pem" ubuntu@52.1.200.223
     ```
  2. Ejecutar `ps aux | grep java` para mostrar los microservicios activos:
     - `ms-vidasalud-catalog` (8081)
     - `ms-vidasalud-appointments` (8082)
     - `ms-vidasalud-audit` (8083)
     - `ms-vidasalud-report` (8084)
     - `ms-vidasalud-notify` (8085)
  3. Mostrar que la base de datos PostgreSQL y los brokers están activos vía `docker ps`.

---

## 🐇 8. Clúster de RabbitMQ de Dos (2) Nodos
* **Panel de Control:** `http://52.1.200.223:15672` (User: `guest` | Pass: `guest`)
* **Demostración:**
  1. En el panel de RabbitMQ, ir a la pestaña **Overview / Nodes** (`http://52.1.200.223:15672/#/nodes`).
  2. Mostrar que existen dos nodos activos en clúster:
     - `rabbit@rabbitmq-node1`
     - `rabbit@rabbitmq-node2`
  3. Mostrar la sincronización y alta disponibilidad entre ambos nodos configurados en `infra/docker-compose.yml` y `infra/rabbitmq.conf`.

---

## 📬 9. Tres (3) Colas de Mensajes con sus Respectivas DLQ
* **Demostración:**
  1. En el panel de RabbitMQ, ir a la pestaña **Queues** (`http://52.1.200.223:15672/#/queues`).
  2. Mostrar las **3 colas principales** con sus argumentos `x-dead-letter-exchange`:
     - `audit.queue` $\rightarrow$ Enrutada a `audit.dlq`
     - `notify.queue` $\rightarrow$ Enrutada a `notify.dlq`
     - `direct.queue` $\rightarrow$ Enrutada a `direct.dlq`
  3. Mostrar las **3 Dead Letter Queues (DLQ)** creadas y en espera de mensajes con error:
     - `audit.dlq`
     - `notify.dlq`
     - `direct.dlq`

---

## 🔀 10. Exchanges de tipo DIRECT y TOPIC
* **Demostración:**
  1. En el panel de RabbitMQ, ir a la pestaña **Exchanges** (`http://52.1.200.223:15672/#/exchanges`).
  2. Mostrar claramente en la columna **Type**:
     - **Exchange tipo Direct:**
       - `direct.exchange` (Type: **direct**, vinculado a `direct.queue` con key `direct.routing.key`)
     - **Exchanges tipo Topic:**
       - `audit.exchange` (Type: **topic**, vinculado a `audit.queue` con key `audit.routing.key`)
       - `notify.exchange` (Type: **topic**, vinculado a `notify.queue` con key `notify.routing.key`)
       - `deadLetterExchange` (Type: **topic**, exchange de DLX vinculado a las 3 DLQs con `dlq.routing.key`)

---

## 🐳 11. Ejecución de Docker-Compose en la Nube (RabbitMQ / Kafka)
* **Demostración:**
  1. En la terminal de la EC2, ejecutar:
     ```bash
     cd ~/Vida-Salud/infra
     docker compose ps
     ```
  2. O levantar los servicios en vivo:
     - Para Clúster RabbitMQ + PostgreSQL:
       ```bash
       docker compose -f docker-compose-rabbitmq-cluster.yml up -d
       ```
     - Para Clúster Kafka (3 brokers) + Kafka-UI:
       ```bash
       docker compose -f docker-compose-kafka.yml up -d
       ```
     - O todo unificado:
       ```bash
       docker compose up -d
       ```
  3. Ejecutar `docker ps` y mostrar los contenedores con estado `Up` y puertos asignados.

---

## 📊 12. Kafka-UI con 2 Tópicos de 3 Particiones y 3 Réplicas
* **Panel Web Kafka-UI:** `http://52.1.200.223:8088` (o `http://localhost:8088`)
* **Demostración:**
  1. Abrir en el navegador: `http://52.1.200.223:8088`
  2. Mostrar en el menú superior o dashboard el clúster `vidasalud-cluster` con **3 Brokers en línea** (`kafka-1`, `kafka-2`, `kafka-3`).
  3. Ir a la pestaña **Topics (Tópicos)**.
  4. Mostrar los 2 tópicos creados:
     - `vidasalud-citas`:
       - **Partitions:** `3`
       - **Replication Factor:** `3`
     - `vidasalud-alertas`:
       - **Partitions:** `3`
       - **Replication Factor:** `3`
  5. *(Opcional para lucirse ante el profesor)*: Pulsar el botón **"Add a Topic"**, ingresar un nombre (ej. `test-evaluacion`), asignar `3` particiones y `3` de factor de replicación, y pulsar **Create Topic** en tiempo real.

