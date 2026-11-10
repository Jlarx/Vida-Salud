# 🔒 Microservicio Auditoría - VidaSalud

Trazabilidad inmutable de cambios de estado y acciones operativas de la plataforma clínica.

## 🚀 Capacidades
- **Consumidor Asíncrono AMQP**: Escucha eventos de atención médica desde `audit.queue`.
- **Tolerancia a Fallos**: Cola de descarte `audit.dlq` para mensajes no procesables.
- **Historial de Auditoría**: Exposición de registros históricos para supervisión médica.
- **Persistencia**: PostgreSQL (`vidasalud_audit`).
- **Seguridad**: Validación JWT de Azure Entra ID.
- **Puerto**: `8083`.
