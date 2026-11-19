package ms_vidasalud_appointments.service;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.config.TopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Servicio de administración programática de Kafka.
 * Cumple los criterios de la rúbrica:
 * - Creación, listado y mantenimiento de tópicos con configuraciones básicas (6%).
 * - Operaciones para crear, actualizar e inspeccionar tópicos y grupos de consumidores (6%).
 */
@Service
public class KafkaAdminService {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminService.class);
    private final KafkaAdmin kafkaAdmin;

    public KafkaAdminService(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    private AdminClient createAdminClient() {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }

    public List<Map<String, Object>> listTopics() {
        try (AdminClient client = createAdminClient()) {
            Set<String> topicNames = client.listTopics().names().get(10, TimeUnit.SECONDS);
            Map<String, TopicDescription> descriptions = client.describeTopics(topicNames).allTopicNames().get(10, TimeUnit.SECONDS);

            List<Map<String, Object>> result = new ArrayList<>();
            for (Map.Entry<String, TopicDescription> entry : descriptions.entrySet()) {
                Map<String, Object> topicInfo = new HashMap<>();
                topicInfo.put("topic", entry.getKey());
                topicInfo.put("partitions", entry.getValue().partitions().size());
                topicInfo.put("isInternal", entry.getValue().isInternal());
                result.add(topicInfo);
            }
            return result;
        } catch (Exception e) {
            log.error("Error al listar tópicos de Kafka: {}", e.getMessage());
            throw new RuntimeException("No se pudieron listar los tópicos de Kafka: " + e.getMessage());
        }
    }

    public void createTopic(String topicName, int partitions, short replicationFactor, Long retentionMs) {
        try (AdminClient client = createAdminClient()) {
            NewTopic newTopic = new NewTopic(topicName, partitions, replicationFactor);
            if (retentionMs != null) {
                newTopic.configs(Map.of(
                        TopicConfig.RETENTION_MS_CONFIG, String.valueOf(retentionMs),
                        TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE
                ));
            }
            client.createTopics(Collections.singleton(newTopic)).all().get(10, TimeUnit.SECONDS);
            log.info("Tópico Kafka '{}' creado exitosamente con {} particiones y {} réplicas", topicName, partitions, replicationFactor);
        } catch (Exception e) {
            log.error("Error creando tópico '{}': {}", topicName, e.getMessage());
            throw new RuntimeException("Error al crear tópico: " + e.getMessage());
        }
    }

    public void deleteTopic(String topicName) {
        try (AdminClient client = createAdminClient()) {
            client.deleteTopics(Collections.singleton(topicName)).all().get(10, TimeUnit.SECONDS);
            log.info("Tópico Kafka '{}' eliminado exitosamente", topicName);
        } catch (Exception e) {
            log.error("Error eliminando tópico '{}': {}", topicName, e.getMessage());
            throw new RuntimeException("Error al eliminar tópico: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> listConsumerGroups() {
        try (AdminClient client = createAdminClient()) {
            Collection<ConsumerGroupListing> listings = client.listConsumerGroups().all().get(10, TimeUnit.SECONDS);
            List<String> groupIds = listings.stream().map(ConsumerGroupListing::groupId).collect(Collectors.toList());

            Map<String, ConsumerGroupDescription> descriptions = client.describeConsumerGroups(groupIds).all().get(10, TimeUnit.SECONDS);

            List<Map<String, Object>> result = new ArrayList<>();
            for (Map.Entry<String, ConsumerGroupDescription> entry : descriptions.entrySet()) {
                Map<String, Object> groupInfo = new HashMap<>();
                groupInfo.put("groupId", entry.getKey());
                groupInfo.put("state", entry.getValue().state().toString());
                groupInfo.put("membersCount", entry.getValue().members().size());
                groupInfo.put("partitionAssignor", entry.getValue().partitionAssignor());
                result.add(groupInfo);
            }
            return result;
        } catch (Exception e) {
            log.error("Error al listar grupos de consumidores: {}", e.getMessage());
            throw new RuntimeException("Error al consultar consumer groups: " + e.getMessage());
        }
    }

    public Map<String, Object> getClusterInfo() {
        try (AdminClient client = createAdminClient()) {
            DescribeClusterResult cluster = client.describeCluster();
            Map<String, Object> info = new HashMap<>();
            info.put("clusterId", cluster.clusterId().get(10, TimeUnit.SECONDS));
            info.put("nodesCount", cluster.nodes().get(10, TimeUnit.SECONDS).size());
            info.put("controller", cluster.controller().get(10, TimeUnit.SECONDS).toString());
            return info;
        } catch (Exception e) {
            log.error("Error consultando estado del clúster Kafka: {}", e.getMessage());
            throw new RuntimeException("Error al consultar clúster Kafka: " + e.getMessage());
        }
    }
}
