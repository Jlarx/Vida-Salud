package ms_vidasalud_appointments.controller;

import ms_vidasalud_appointments.entity.Atencion;
import ms_vidasalud_appointments.entity.EstadoAtencion;
import ms_vidasalud_appointments.repository.AtencionRepository;
import ms_vidasalud_appointments.service.AtencionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AtencionController {

    private final AtencionRepository atencionRepository;
    private final AtencionService atencionService;

    public AtencionController(AtencionRepository atencionRepository, AtencionService atencionService) {
        this.atencionRepository = atencionRepository;
        this.atencionService = atencionService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PACIENTE', 'RECEPCIONISTA', 'ADMINISTRADOR')")
    public List<Atencion> getAllAtenciones() {
        return atencionRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('PACIENTE') or hasRole('RECEPCIONISTA')")
    public Atencion solicitarAtencion(@RequestBody Atencion atencion, @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("preferred_username"); // Campo común en tokens de Azure AD
        if (email == null) email = jwt.getSubject();
        
        atencion.setPacienteEmail(email);
        atencion.setEstado(EstadoAtencion.SOLICITADA);
        return atencionRepository.save(atencion);
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasRole('RECEPCIONISTA')") // Solo Recepcionista cambia estados operativos
    public Atencion cambiarEstado(@PathVariable Long id, @RequestParam EstadoAtencion nuevoEstado, @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("preferred_username");
        if (email == null) email = jwt.getSubject();
        
        return atencionService.cambiarEstado(id, nuevoEstado, email);
    }
}
