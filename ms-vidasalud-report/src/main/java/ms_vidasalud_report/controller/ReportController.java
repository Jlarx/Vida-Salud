package ms_vidasalud_report.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/api/report", "/report", ""})
public class ReportController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "ms-vidasalud-report");
        status.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(status);
    }

    @GetMapping({"", "/resumen"})
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'AUDITOR')")
    public ResponseEntity<Map<String, Object>> getResumen() {
        Map<String, Object> resumen = new HashMap<>();
        resumen.put("timestamp", LocalDateTime.now().toString());
        resumen.put("modulo", "Reportes y Estadísticas VidaSalud");
        resumen.put("estado", "OPERACIONAL");
        return ResponseEntity.ok(resumen);
    }
}

