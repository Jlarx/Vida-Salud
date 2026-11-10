# 🔔 Microservicio Notificaciones - VidaSalud

Gestión y emisión de alertas hacia pacientes y personal asistencial.

## 🚀 Capacidades
- **Consumidor AMQP**: Suscripción al tópico de notificaciones en `notify.queue`.
- **Enrutamiento por Tópico**: Procesamiento selectivo de eventos según routing key.
- **Cola DLQ**: Enrutamiento a `notify.dlq` si el mensaje no puede ser notificado.
- **Seguridad**: Validación JWT de Azure Entra ID.
- **Puerto**: `8085`.
