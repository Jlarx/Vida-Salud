package ms_vidasalud_appointments.messaging.producer;

import ms_vidasalud_appointments.dto.AlertaKafkaEventDTO;
import ms_vidasalud_appointments.dto.AuditoriaEvent;
import ms_vidasalud_appointments.dto.CitaKafkaEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class AtencionKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(AtencionKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.citas:vidasalud-citas}")
    private String topicCitas;

    @Value("${kafka.topic.alertas:vidasalud-alertas}")
    private String topicAlertas;

    public AtencionKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishCitaEvent(String atencionId, AuditoriaEvent event) {
        CitaKafkaEventDTO dto = CitaKafkaEventDTO.builder()
                .atencionId(event.getAtencionId())
                .pacienteEmail(event.getUsuarioEmail())
                .usuarioResponsable(event.getUsuarioEmail())
                .estadoAnterior(event.getEstadoAnterior())
                .estadoNuevo(event.getEstadoNuevo())
                .timestamp(event.getFechaCambio() != null ? event.getFechaCambio().toString()
                        : LocalDateTime.now().toString())
                .build();
        publishCitaDTO(atencionId, dto);
    }

    public void publishCitaDTO(String atencionId, CitaKafkaEventDTO dto) {
        try {
            kafkaTemplate.send(topicCitas, atencionId, dto);
            log.info("[KAFKA - Producer] Evento tipado publicado en '{}' [Key: {}]: Estado {} -> {}",
                    topicCitas, atencionId, dto.getEstadoAnterior(), dto.getEstadoNuevo());
        } catch (Exception e) {
            log.error("[KAFKA - Producer] Error al enviar CitaKafkaEventDTO a Kafka: {}", e.getMessage(), e);
        }
    }

    public void publishAlertaEvent(String atencionId, String mensajeAlerta) {
        AlertaKafkaEventDTO dto = AlertaKafkaEventDTO.builder()
                .alertaId(UUID.randomUUID().toString())
                .atencionId(Long.valueOf(atencionId))
                .tipoAlerta("CAMBIO_ESTADO")
                .nivelPrioridad("ALTA")
                .mensaje(mensajeAlerta)
                .destinatario("sistema-alertas@vidasalud.cl")
                .fechaEmision(LocalDateTime.now().toString())
                .build();
        publishAlertaDTO(atencionId, dto);
    }

    public void publishAlertaDTO(String atencionId, AlertaKafkaEventDTO dto) {
        try {
            kafkaTemplate.send(topicAlertas, atencionId, dto);
            log.info("[KAFKA - Producer] Alerta tipada publicada en '{}' [Key: {}]: {}",
                    topicAlertas, atencionId, dto.getMensaje());
        } catch (Exception e) {
            log.error("[KAFKA - Producer] Error al enviar AlertaKafkaEventDTO a Kafka: {}", e.getMessage(), e);
        }
    }
}
