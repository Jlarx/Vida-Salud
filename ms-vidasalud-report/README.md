# 📊 Microservicio Reportes y Métricas - VidaSalud

Generación de indicadores de rendimiento clínico y consolidación estadística.

## 🚀 Capacidades
- **Consumidor Directo AMQP**: Recibe eventos clave desde `direct.queue` mediante `direct.exchange`.
- **Confirmación Manual (ACK)**: Garantía de entrega exactly-once con redirección a `direct.dlq` ante excepciones.
- **Endpoints de Reportería**: `/report/health` y `/report/resumen`.
- **Persistencia**: PostgreSQL (`vidasalud_report`).
- **Seguridad**: Validación JWT de Azure Entra ID.
- **Puerto**: `8084`.

