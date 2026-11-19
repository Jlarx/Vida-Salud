package ms_vidasalud_notify.dto;

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
    private String tipoAlerta;
    private String nivelPrioridad;
    private String mensaje;
    private String destinatario;
    private String fechaEmision;
}
