package ms_vidasalud_appointments.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExchangeRequestDTO {
    @NotBlank(message = "El nombre del exchange no puede estar vacío")
    private String exchangeName;
    
    @NotBlank(message = "El tipo de exchange no puede estar vacío")
    private String type; // topic, direct, etc.
}
