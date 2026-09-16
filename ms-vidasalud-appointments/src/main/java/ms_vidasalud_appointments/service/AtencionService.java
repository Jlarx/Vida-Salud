package ms_vidasalud_appointments.service;

import ms_vidasalud_appointments.entity.*;
import ms_vidasalud_appointments.repository.AtencionRepository;
// import ms_vidasalud_appointments.repository.AuditoriaAtencionRepository;
// import ms_vidasalud_appointments.repository.BoxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtencionService {
    
    private final AtencionRepository atencionRepository;
    public AtencionService(AtencionRepository atencionRepository) {
        this.atencionRepository = atencionRepository;
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
        // TODO: Enviar evento a Kafka en lugar de guardar en BD síncronamente

        return guardada;
    }
}
