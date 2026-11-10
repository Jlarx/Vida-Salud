# 📅 Microservicio Citas y Atenciones - VidaSalud

Núcleo transaccional para la reserva, reprogramación y seguimiento de atenciones médicas.

## 🚀 Capacidades
- **Gestión de Citas**: Ciclo de vida completo (CREADA, CONFIRMADA, ATENDIDA, CANCELADA).
- **Publicación AMQP (RabbitMQ)**:
  - Eventos de auditoría en `audit.exchange` (topic).
  - Eventos de notificación en `notify.exchange` (topic).
  - Enrutamiento directo a reportería en `direct.exchange` (direct).
- **Dead Letter Queues**: Redirección automática a DLQ ante fallos de consumo.
- **Persistencia**: PostgreSQL (`vidasalud_appointments`).
- **Seguridad**: Validación JWT de Azure Entra ID con control de roles por método.
- **Puerto**: `8082`.
