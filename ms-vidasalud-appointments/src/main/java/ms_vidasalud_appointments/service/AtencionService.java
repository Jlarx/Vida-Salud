package ms_vidasalud_appointments.service;

import ms_vidasalud_appointments.dto.AuditoriaEvent;
import ms_vidasalud_appointments.entity.Atencion;
import ms_vidasalud_appointments.entity.EstadoAtencion;
import ms_vidasalud_appointments.messaging.producer.AtencionEventPublisher;
import ms_vidasalud_appointments.messaging.producer.AtencionKafkaProducer;
import ms_vidasalud_appointments.repository.AtencionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AtencionService {
    
    private final AtencionRepository atencionRepository;
    private final AtencionEventPublisher eventPublisher;
    private final AtencionKafkaProducer kafkaProducer;

    public AtencionService(AtencionRepository atencionRepository, AtencionEventPublisher eventPublisher, AtencionKafkaProducer kafkaProducer) {
        this.atencionRepository = atencionRepository;
        this.eventPublisher = eventPublisher;
        this.kafkaProducer = kafkaProducer;
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
        
        // Validación de secuencia: permitir al operador transicionar estados según la dinámica de la clínica
        if (nuevoEstado == EstadoAtencion.EN_ATENCION && estadoAnterior == EstadoAtencion.CERRADA) {
            throw new RuntimeException("No se puede reactivar una cita que ya ha sido CERRADA.");
        }
        
        // Liberar cupo al CERRAR o CANCELAR
        if ((nuevoEstado == EstadoAtencion.CERRADA || nuevoEstado == EstadoAtencion.CANCELADA) && atencion.getBoxId() != null) {
            // TODO: Llamar a Catalog-Service vía Feign o RestTemplate para sumar cupo
        }

        atencion.setEstado(nuevoEstado);
        Atencion guardada = atencionRepository.save(atencion);

        // Registro de Auditoría
        String antEstadoNombre = estadoAnterior != null ? estadoAnterior.name() : "INICIAL";
        AuditoriaEvent event = new AuditoriaEvent(
                guardada.getId(),
                usuarioEmail != null ? usuarioEmail : "sistema@vidasalud.cl",
                antEstadoNombre,
                nuevoEstado.name(),
                LocalDateTime.now()
        );
        eventPublisher.publishAudit(event);
        eventPublisher.publishNotify(event);
        eventPublisher.publishDirect(event);

        // Publicación en Kafka (Clave = ID de atención para particionamiento consistente)
        kafkaProducer.publishCitaEvent(String.valueOf(guardada.getId()), event);
        if (nuevoEstado == EstadoAtencion.CANCELADA || nuevoEstado == EstadoAtencion.CONFIRMADA) {
            kafkaProducer.publishAlertaEvent(String.valueOf(guardada.getId()), "ESTADO CITA #" + guardada.getId() + ": " + nuevoEstado);
        }

        return guardada;
    }

    @Transactional
    public Atencion reagendarCita(Long id, LocalDateTime nuevaFecha, String usuarioEmail) {
        Atencion atencion = atencionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Atención no encontrada"));

        atencion.setFechaCita(nuevaFecha);
        Atencion guardada = atencionRepository.save(atencion);

        AuditoriaEvent event = new AuditoriaEvent(
                guardada.getId(),
                usuarioEmail,
                "REAGENDAMIENTO",
                "Nueva fecha: " + nuevaFecha.toString(),
                LocalDateTime.now()
        );
        eventPublisher.publishAudit(event);
        eventPublisher.publishNotify(event);
        eventPublisher.publishDirect(event);

        // Publicación en Kafka
        kafkaProducer.publishCitaEvent(String.valueOf(guardada.getId()), event);
        kafkaProducer.publishAlertaEvent(String.valueOf(guardada.getId()), "REAGENDAMIENTO CITA #" + guardada.getId() + " para " + nuevaFecha);

        return guardada;
    }
}
