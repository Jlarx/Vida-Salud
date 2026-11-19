package ms_vidasalud_appointments.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuración explícita de Tópicos Kafka administrados por código.
 * Cumple con el criterio de administración de tópicos:
 * - 3 particiones y factor de replicación 3.
 * - Nivel de retención explícito (7 días = 604800000 ms).
 * - Cleanup policy (delete).
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092,localhost:9094,localhost:9096}")
    private String bootstrapServers;

    @Value("${kafka.topic.citas:vidasalud-citas}")
    private String topicCitasName;

    @Value("${kafka.topic.alertas:vidasalud-alertas}")
    private String topicAlertasName;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic topicVidasaludCitas() {
        return TopicBuilder.name(topicCitasName)
                .partitions(3)
                .replicas(3)
                .config(TopicConfig.RETENTION_MS_CONFIG, "604800000") // 7 días en milisegundos
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .build();
    }

    @Bean
    public NewTopic topicVidasaludAlertas() {
        return TopicBuilder.name(topicAlertasName)
                .partitions(3)
                .replicas(3)
                .config(TopicConfig.RETENTION_MS_CONFIG, "604800000") // 7 días en milisegundos
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .build();
    }
}
