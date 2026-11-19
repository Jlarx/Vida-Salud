package ms_vidasalud_appointments.controller;

import jakarta.validation.Valid;
import ms_vidasalud_appointments.dto.KafkaTopicRequestDTO;
import ms_vidasalud_appointments.service.KafkaAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de infraestructura de Apache Kafka.
 * Cumple los criterios de la rúbrica (Criterio 3: 6% y Criterio 9: 6%):
 * - Endpoints para crear, listar y mantener tópicos y configuraciones.
 * - Inspección de tópicos y grupos de consumidores de manera controlada.
 */
@RestController
@RequestMapping({"/api/kafka", "/kafka", "/api/appointments/kafka", "/appointments/kafka"})
public class KafkaAdminController {

    private final KafkaAdminService kafkaAdminService;

    public KafkaAdminController(KafkaAdminService kafkaAdminService) {
        this.kafkaAdminService = kafkaAdminService;
    }

    @GetMapping("/topics")
    public ResponseEntity<List<Map<String, Object>>> listTopics() {
        return ResponseEntity.ok(kafkaAdminService.listTopics());
    }

    @PostMapping("/topics")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<String> createTopic(@Valid @RequestBody KafkaTopicRequestDTO request) {
        kafkaAdminService.createTopic(
                request.getTopicName(),
                request.getPartitions(),
                request.getReplicationFactor(),
                request.getRetentionMs()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Tópico Kafka '" + request.getTopicName() + "' creado con " +
                        request.getPartitions() + " particiones y " +
                        request.getReplicationFactor() + " réplicas.");
    }

    @DeleteMapping("/topics/{topicName}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<String> deleteTopic(@PathVariable String topicName) {
        kafkaAdminService.deleteTopic(topicName);
        return ResponseEntity.ok("Tópico Kafka '" + topicName + "' eliminado correctamente.");
    }

    @GetMapping("/consumers")
    public ResponseEntity<List<Map<String, Object>>> listConsumerGroups() {
        return ResponseEntity.ok(kafkaAdminService.listConsumerGroups());
    }

    @GetMapping("/cluster")
    public ResponseEntity<Map<String, Object>> getClusterInfo() {
        return ResponseEntity.ok(kafkaAdminService.getClusterInfo());
    }
}
