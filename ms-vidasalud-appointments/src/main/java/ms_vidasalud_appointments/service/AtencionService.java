package ms_vidasalud_appointments.service;

import ms_vidasalud_appointments.entity.*;
import ms_vidasalud_appointments.repository.AtencionRepository;
// import ms_vidasalud_appointments.repository.AuditoriaAtencionRepository;
// import ms_vidasalud_appointments.repository.BoxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ms_vidasalud_appointments.messaging.producer.AtencionEventPublisher;
import ms_vidasalud_appointments.dto.AuditoriaEvent;
import java.time.LocalDateTime;

@Service
public class AtencionService {
    
    private final AtencionRepository atencionRepository;
    private final AtencionEventPublisher eventPublisher;

    public AtencionService(AtencionRepository atencionRepository, AtencionEventPublisher eventPublisher) {
        this.atencionRepository = atencionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Atencion cambiarEstado(Long id, EstadoAtencion nuevoEstado, String usuarioEmail) {
        Atencion atencion = atencionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Atención no encontrada"));
        
        EstadoAtencion estadoAnterior = atencion.getEstado();
        
        // Validación: De SOLICITADA a CONFIRMADA, necesita descontar un cupo si tiene Box asignado
        if (nuevoEstado == EstadoAtencion.CONFIRMADA && estadoAnterior == EstadoAtencion.SOLICITADA) {
            if (atencion.getBoxId() != null) {
                // TODO: Llamar a Catalog-Service vía Feign o RestTemplate para descontar cupo
            }
        }
        
        // Validación de secuencia (ejemplo simple)
        if (nuevoEstado == EstadoAtencion.EN_ATENCION && estadoAnterior != EstadoAtencion.CONFIRMADA && estadoAnterior != EstadoAtencion.EN_ESPERA) {
            throw new RuntimeException("No se puede pasar a EN_ATENCION si no está CONFIRMADA o EN_ESPERA");
        }
        
        // Liberar cupo al CERRAR o CANCELAR
        if ((nuevoEstado == EstadoAtencion.CERRADA || nuevoEstado == EstadoAtencion.CANCELADA) && atencion.getBoxId() != null) {
            // TODO: Llamar a Catalog-Service vía Feign o RestTemplate para sumar cupo
        }

        atencion.setEstado(nuevoEstado);
        Atencion guardada = atencionRepository.save(atencion);

        // Registro de Auditoría
        AuditoriaEvent event = new AuditoriaEvent(
                guardada.getId(),
                usuarioEmail,
                estadoAnterior.name(),
                nuevoEstado.name(),
                LocalDateTime.now()
        );
        eventPublisher.publishAudit(event);
        eventPublisher.publishNotify(event);

        return guardada;
    }
}
