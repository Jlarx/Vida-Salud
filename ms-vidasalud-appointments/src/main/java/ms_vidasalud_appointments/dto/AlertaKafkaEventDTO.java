package ms_vidasalud_appointments.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertaKafkaEventDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String alertaId;
    private Long atencionId;
    private String tipoAlerta;     // EJ: "CITA_SOLICITADA", "CITA_CONFIRMADA", "CITA_CANCELADA", "CITA_REAGENDADA"
    private String nivelPrioridad; // "ALTA", "MEDIA", "INFORMATIVA"
    private String mensaje;
    private String destinatario;
    private String fechaEmision;
}
