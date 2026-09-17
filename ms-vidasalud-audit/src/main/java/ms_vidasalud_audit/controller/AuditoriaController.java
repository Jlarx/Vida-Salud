package ms_vidasalud_audit.controller;

import ms_vidasalud_audit.entity.AuditoriaAtencion;
import ms_vidasalud_audit.repository.AuditoriaAtencionRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('AUDITOR')") // Solo el rol Auditor puede ver estos datos
public class AuditoriaController {

    private final AuditoriaAtencionRepository auditoriaRepository;

    public AuditoriaController(AuditoriaAtencionRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    @GetMapping("/timeline/{atencionId}")
    public List<AuditoriaAtencion> getTimeline(@PathVariable Long atencionId) {
        return auditoriaRepository.findByAtencionIdOrderByFechaCambioDesc(atencionId);
    }
}
