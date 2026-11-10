# 🐳 Infraestructura Docker - VidaSalud

Orquestación de servicios base en la nube (AWS EC2).

## 📦 Componentes
1. **PostgreSQL 15**: Base de datos relacional multi-esquema (`vidasalud_catalog`, `vidasalud_appointments`, `vidasalud_audit`, `vidasalud_report`).
2. **RabbitMQ Clúster (2 Nodos)**:
   - `rabbitmq-node1` (Puerto AMQP 5672, Management 15672)
   - `rabbitmq-node2` (Puerto AMQP 5673, Management 15673)
   - Exchanges `direct` y `topic`, 3 colas de negocio y 3 Dead Letter Queues (DLQ).
3. **Apache Kafka Clúster (KRaft - 3 Brokers)**:
   - `kafka-1` (9092), `kafka-2` (9094), `kafka-3` (9096)
   - Tópicos con 3 particiones y factor de replicación 3 (`vidasalud-citas`, `vidasalud-alertas`).
4. **Kafka-UI**: Panel de monitoreo web accesible en el puerto **8088**.
