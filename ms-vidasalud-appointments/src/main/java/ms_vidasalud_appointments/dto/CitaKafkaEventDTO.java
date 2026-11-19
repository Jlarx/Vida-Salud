package ms_vidasalud_appointments.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaKafkaEventDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long atencionId;
    private String pacienteEmail;
    private String estadoAnterior;
    private String estadoNuevo;
    private Long prestacionId;
    private Long boxId;
    private String fechaCita;
    private String usuarioResponsable;
    private String timestamp;
}
