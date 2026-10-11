package ms_vidasalud_appointments.controller;

import jakarta.validation.Valid;
import ms_vidasalud_appointments.dto.ExchangeRequestDTO;
import ms_vidasalud_appointments.dto.QueueRequestDTO;
import ms_vidasalud_appointments.service.RabbitAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class RabbitAdminController {

    private final RabbitAdminService rabbitAdminService;

    public RabbitAdminController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    @PostMapping({"/queues", "/api/rabbitmq/queues"})
    public ResponseEntity<String> createQueue(@Valid @RequestBody QueueRequestDTO request) {
        rabbitAdminService.createQueue(request.getQueueName());
        return ResponseEntity.status(HttpStatus.CREATED).body("Cola creada: " + request.getQueueName());
    }

    @DeleteMapping({"/queues/{name}", "/api/rabbitmq/queues/{name}"})
    public ResponseEntity<String> deleteQueue(@PathVariable String name) {
        rabbitAdminService.deleteQueue(name);
        return ResponseEntity.ok("Cola eliminada: " + name);
    }

    @PostMapping({"/exchanges", "/api/rabbitmq/exchanges"})
    public ResponseEntity<String> createExchange(@Valid @RequestBody ExchangeRequestDTO request) {
        rabbitAdminService.createExchange(request.getExchangeName());
        return ResponseEntity.status(HttpStatus.CREATED).body("Exchange creado: " + request.getExchangeName());
    }
}
