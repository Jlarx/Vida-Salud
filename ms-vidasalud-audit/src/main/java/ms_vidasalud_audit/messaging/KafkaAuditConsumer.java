package ms_vidasalud_audit.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ms_vidasalud_audit.dto.CitaKafkaEventDTO;
import ms_vidasalud_audit.entity.AuditoriaAtencion;
import ms_vidasalud_audit.repository.AuditoriaAtencionRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class KafkaAuditConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaAuditConsumer.class);

    private final AuditoriaAtencionRepository repository;
    private final ObjectMapper objectMapper;

    public KafkaAuditConsumer(AuditoriaAtencionRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @KafkaListener(topics = "${kafka.topic.citas:vidasalud-citas}", groupId = "audit-group")
    public void consumeCitaEvent(ConsumerRecord<String, String> record) {
        if (record == null || record.value() == null) {
            log.warn("[KAFKA - audit-group] Registro nulo recibido. Ignorando mensaje de forma segura.");
            return;
        }

        String key = record.key();
        String jsonValue = record.value();

        log.info("[KAFKA - audit-group] Consumiendo evento en Partición: {}, Offset: {}, Key: {}",
                record.partition(), record.offset(), key);

        try {
            CitaKafkaEventDTO dto = objectMapper.readValue(jsonValue, CitaKafkaEventDTO.class);

            Long atencionId = dto.getAtencionId();
            if (atencionId == null && key != null) {
                try {
                    atencionId = Long.valueOf(key);
                } catch (NumberFormatException ignored) {
                }
            }

            if (atencionId == null) {
                log.warn("[KAFKA - audit-group] Evento descartado: atencionId no presente");
                return;
            }

            AuditoriaAtencion auditoria = new AuditoriaAtencion();
            auditoria.setAtencionId(atencionId);
            auditoria.setUsuarioEmail(
                    dto.getUsuarioResponsable() != null ? dto.getUsuarioResponsable() : "kafka-streaming@vidasalud.cl");
            auditoria.setEstadoAnterior(dto.getEstadoAnterior() != null ? dto.getEstadoAnterior() : "INICIAL");
            auditoria.setEstadoNuevo(dto.getEstadoNuevo() != null ? dto.getEstadoNuevo() : "PROCESADO");
            auditoria.setFechaCambio(LocalDateTime.now());

            repository.save(auditoria);
            log.info("[KAFKA - audit-group] Auditoría persistida para Cita #{}: Estado {} -> {}",
                    atencionId, auditoria.getEstadoAnterior(), auditoria.getEstadoNuevo());

        } catch (Exception e) {
            log.error("[KAFKA - audit-group] Error durante el procesamiento del evento en Partición {} Offset {}: {}",
                    record.partition(), record.offset(), e.getMessage());
        }
    }
}
