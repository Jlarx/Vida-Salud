package ms_vidasalud_appointments.controller;

import ms_vidasalud_appointments.entity.Atencion;
import ms_vidasalud_appointments.entity.EstadoAtencion;
import ms_vidasalud_appointments.repository.AtencionRepository;
import ms_vidasalud_appointments.service.AtencionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ms_vidasalud_appointments.messaging.producer.AtencionEventPublisher;
import ms_vidasalud_appointments.dto.AuditoriaEvent;
import java.time.LocalDateTime;

import java.util.List;

@RestController
@RequestMapping({"/api/appointments", "/appointments"})
public class AtencionController {

    private final AtencionRepository atencionRepository;
    private final AtencionService atencionService;
    private final AtencionEventPublisher eventPublisher;

    public AtencionController(AtencionRepository atencionRepository, AtencionService atencionService, AtencionEventPublisher eventPublisher) {
        this.atencionRepository = atencionRepository;
        this.atencionService = atencionService;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'OPERADOR', 'ADMINISTRADOR')")
    public List<Atencion> getAllAtenciones() {
        return atencionRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'OPERADOR', 'ADMINISTRADOR')")
    public Atencion solicitarAtencion(@RequestBody Atencion atencion, @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("preferred_username"); // Campo común en tokens de Azure AD
        if (email == null) email = jwt.getSubject();
        
        atencion.setPacienteEmail(email);
        atencion.setEstado(EstadoAtencion.SOLICITADA);
        Atencion guardada = atencionRepository.save(atencion);

        AuditoriaEvent event = new AuditoriaEvent(
                guardada.getId(),
                email,
                "CREACION",
                EstadoAtencion.SOLICITADA.name(),
                LocalDateTime.now()
        );
        eventPublisher.publishAudit(event);
        eventPublisher.publishNotify(event);

        return guardada;
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMINISTRADOR')") // Solo Operador/Admin cambia estados operativos
    public Atencion cambiarEstado(@PathVariable Long id, @RequestParam EstadoAtencion nuevoEstado, @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("preferred_username");
        if (email == null) email = jwt.getSubject();
        
        return atencionService.cambiarEstado(id, nuevoEstado, email);
    }

    @PutMapping("/{id}/reagendar")
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMINISTRADOR')")
    public Atencion reagendar(@PathVariable Long id, @RequestParam String nuevaFecha, @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("preferred_username");
        if (email == null) email = jwt.getSubject();

        LocalDateTime fechaParsed = LocalDateTime.parse(nuevaFecha);
        return atencionService.reagendarCita(id, fechaParsed, email);
    }
}
