package ms_vidasalud_appointments.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KafkaTopicRequestDTO {

    @NotBlank(message = "El nombre del tópico es obligatorio")
    private String topicName;

    private int partitions = 3;
    private short replicationFactor = 3;
    private Long retentionMs = 604800000L; // 7 días por defecto
}
