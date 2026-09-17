package ms_vidasalud_audit.messaging;

import ms_vidasalud_audit.dto.AuditoriaEvent;
import ms_vidasalud_audit.entity.AuditoriaAtencion;
import ms_vidasalud_audit.repository.AuditoriaAtencionRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumer {

    private final AuditoriaAtencionRepository repository;

    public KafkaConsumer(AuditoriaAtencionRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "vidasalud-audit-events", groupId = "audit-group")
    public void consume(AuditoriaEvent event) {
        AuditoriaAtencion auditoria = new AuditoriaAtencion();
        auditoria.setAtencionId(event.getAtencionId());
        auditoria.setUsuarioEmail(event.getUsuarioEmail());
        auditoria.setEstadoAnterior(event.getEstadoAnterior());
        auditoria.setEstadoNuevo(event.getEstadoNuevo());
        auditoria.setFechaCambio(event.getFechaCambio());
        
        repository.save(auditoria);
        System.out.println("Evento de auditoría guardado exitosamente: " + event.getAtencionId());
    }
}
