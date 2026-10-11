package ms_vidasalud_appointments.messaging.producer;

import ms_vidasalud_appointments.dto.AuditoriaEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AtencionEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.audit}")
    private String auditExchange;

    @Value("${rabbitmq.routing.key.audit}")
    private String auditRoutingKey;
    
    @Value("${rabbitmq.exchange.notify}")
    private String notifyExchange;

    @Value("${rabbitmq.routing.key.notify}")
    private String notifyRoutingKey;

    public AtencionEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishAudit(AuditoriaEvent event) {
        rabbitTemplate.convertAndSend(auditExchange, auditRoutingKey, event);
    }
    
    public void publishNotify(AuditoriaEvent event) {
        rabbitTemplate.convertAndSend(notifyExchange, notifyRoutingKey, event);
    }
}
