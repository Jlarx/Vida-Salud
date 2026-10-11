package ms_vidasalud_appointments.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QueueRequestDTO {
    @NotBlank(message = "El nombre de la cola no puede estar vacío")
    private String queueName;
}
