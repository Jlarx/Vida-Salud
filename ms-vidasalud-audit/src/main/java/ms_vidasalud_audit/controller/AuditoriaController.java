package ms_vidasalud_audit.controller;

import ms_vidasalud_audit.entity.AuditoriaAtencion;
import ms_vidasalud_audit.repository.AuditoriaAtencionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/audit", "/audit", ""})
@PreAuthorize("hasAnyRole('AUDITOR', 'ADMINISTRADOR')")
public class AuditoriaController {

    private final AuditoriaAtencionRepository auditoriaRepository;

    public AuditoriaController(AuditoriaAtencionRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    @GetMapping({"", "/auditorias"})
    public List<AuditoriaAtencion> getAllAuditorias() {
        return auditoriaRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaCambio"));
    }

    @GetMapping("/timeline/{atencionId}")
    public List<AuditoriaAtencion> getTimeline(@PathVariable Long atencionId) {
        return auditoriaRepository.findByAtencionIdOrderByFechaCambioDesc(atencionId);
    }
}
