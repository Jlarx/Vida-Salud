package ms_vidasalud_appointments.controller;

import ms_vidasalud_appointments.dto.AuditoriaEvent;
import ms_vidasalud_appointments.entity.Atencion;
import ms_vidasalud_appointments.entity.EstadoAtencion;
import ms_vidasalud_appointments.messaging.producer.AtencionEventPublisher;
import ms_vidasalud_appointments.repository.AtencionRepository;
import ms_vidasalud_appointments.service.AtencionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping({"", "/atenciones"})
    @PreAuthorize("hasAnyRole('CLIENTE', 'OPERADOR', 'ADMINISTRADOR', 'PACIENTE', 'RECEPCIONISTA')")
    public List<Atencion> getAllAtenciones(@AuthenticationPrincipal Jwt jwt) {
        if (jwt != null) {
            List<String> roles = jwt.getClaimAsStringList("roles");
            boolean isStaff = roles != null && roles.stream()
                    .anyMatch(r -> r.equalsIgnoreCase("ADMINISTRADOR") 
                                || r.equalsIgnoreCase("OPERADOR") 
                                || r.equalsIgnoreCase("RECEPCIONISTA")
                                || r.equalsIgnoreCase("AUDITOR"));
            if (!isStaff) {
                String email = jwt.getClaimAsString("preferred_username");
                if (email == null) email = jwt.getSubject();
                if (email != null && !email.isBlank()) {
                    return atencionRepository.findByPacienteEmail(email);
                }
            }
        }
        return atencionRepository.findAll();
    }

    @GetMapping({"/{id}", "/atenciones/{id}"})
    @PreAuthorize("hasAnyRole('CLIENTE', 'OPERADOR', 'ADMINISTRADOR', 'PACIENTE', 'RECEPCIONISTA')")
    public Atencion getAtencionById(@PathVariable Long id) {
        return atencionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Atención con ID " + id + " no encontrada"));
    }

    @GetMapping({"/paciente/{email}", "/atenciones/paciente/{email}"})
    @PreAuthorize("hasAnyRole('CLIENTE', 'OPERADOR', 'ADMINISTRADOR', 'PACIENTE', 'RECEPCIONISTA')")
    public List<Atencion> getAtencionesByPaciente(@PathVariable String email) {
        return atencionRepository.findByPacienteEmail(email);
    }

    @PostMapping({"", "/atenciones"})
    @PreAuthorize("hasAnyRole('CLIENTE', 'OPERADOR', 'ADMINISTRADOR', 'PACIENTE', 'RECEPCIONISTA')")
    public Atencion solicitarAtencion(@RequestBody Atencion atencion, @AuthenticationPrincipal Jwt jwt) {
        String email = jwt != null ? jwt.getClaimAsString("preferred_username") : null;
        if (email == null && jwt != null) email = jwt.getSubject();
        
        // Si no se proporcionó pacienteEmail en el body, usamos el email del JWT
        if (atencion.getPacienteEmail() == null || atencion.getPacienteEmail().isBlank()) {
            atencion.setPacienteEmail(email != null ? email : "paciente@vidasalud.cl");
        }
        
        if (atencion.getEstado() == null) {
            atencion.setEstado(EstadoAtencion.SOLICITADA);
        }
        
        Atencion guardada = atencionRepository.save(atencion);

        String autorCambio = email != null ? email : atencion.getPacienteEmail();
        AuditoriaEvent event = new AuditoriaEvent(
                guardada.getId(),
                autorCambio,
                "CREACION",
                guardada.getEstado().name(),
                LocalDateTime.now()
        );
        eventPublisher.publishAudit(event);
        eventPublisher.publishNotify(event);
        eventPublisher.publishDirect(event);

        return guardada;
    }

    @PutMapping({"/{id}/estado", "/atenciones/{id}/estado"})
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMINISTRADOR', 'RECEPCIONISTA')")
    public Atencion cambiarEstado(
            @PathVariable Long id, 
            @RequestParam(required = false) EstadoAtencion nuevoEstado,
            @RequestParam(required = false) EstadoAtencion estado,
            @AuthenticationPrincipal Jwt jwt) {
        
        EstadoAtencion target = nuevoEstado != null ? nuevoEstado : estado;
        if (target == null) {
            throw new IllegalArgumentException("Debe indicar el nuevo estado ('nuevoEstado' o 'estado')");
        }

        String email = jwt != null ? jwt.getClaimAsString("preferred_username") : null;
        if (email == null && jwt != null) email = jwt.getSubject();
        if (email == null) email = "operador@vidasalud.cl";
        
        return atencionService.cambiarEstado(id, target, email);
    }

    @PutMapping({"/{id}/reagendar", "/atenciones/{id}/reagendar"})
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMINISTRADOR', 'RECEPCIONISTA')")
    public Atencion reagendar(
            @PathVariable Long id, 
            @RequestParam String nuevaFecha, 
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt != null ? jwt.getClaimAsString("preferred_username") : null;
        if (email == null && jwt != null) email = jwt.getSubject();
        if (email == null) email = "operador@vidasalud.cl";

        LocalDateTime fechaParsed = LocalDateTime.parse(nuevaFecha);
        return atencionService.reagendarCita(id, fechaParsed, email);
    }
}
