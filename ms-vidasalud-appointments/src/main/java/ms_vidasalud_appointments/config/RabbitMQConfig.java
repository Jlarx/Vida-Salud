package ms_vidasalud_appointments.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 1. Topic Exchange y Cola de Auditoría
    @Value("${rabbitmq.queue.audit}")
    private String auditQueueName;

    @Value("${rabbitmq.exchange.audit}")
    private String auditExchangeName;

    @Value("${rabbitmq.routing.key.audit}")
    private String auditRoutingKey;

    // 2. Topic Exchange y Cola de Notificaciones
    @Value("${rabbitmq.exchange.notify}")
    private String notifyExchangeName;

    @Value("${rabbitmq.queue.notify}")
    private String notifyQueueName;

    @Value("${rabbitmq.routing.key.notify}")
    private String notifyRoutingKey;

    // 3. Direct Exchange y Cola Directa (Rúbrica: Exchanges Direct y Topic)
    @Value("${rabbitmq.exchange.direct}")
    private String directExchangeName;

    @Value("${rabbitmq.queue.direct}")
    private String directQueueName;

    @Value("${rabbitmq.routing.key.direct}")
    private String directRoutingKey;

    // Configuración de Dead Letter Exchange y 3 DLQs
    @Value("${rabbitmq.dlx}")
    private String dlxName;

    @Value("${rabbitmq.dlq.audit}")
    private String auditDlqName;

    @Value("${rabbitmq.dlq.notify}")
    private String notifyDlqName;

    @Value("${rabbitmq.dlq.direct}")
    private String directDlqName;

    @Value("${rabbitmq.routing.key.dlq}")
    private String dlqRoutingKey;

    // ==========================================
    // COLA 1 (Topic): audit.queue con su DLQ
    // ==========================================
    @Bean
    public Queue auditQueue() {
        return QueueBuilder.durable(auditQueueName)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .build();
    }

    @Bean
    public TopicExchange auditExchange() {
        return new TopicExchange(auditExchangeName);
    }

    @Bean
    public Binding auditBinding(Queue auditQueue, TopicExchange auditExchange) {
        return BindingBuilder.bind(auditQueue).to(auditExchange).with(auditRoutingKey);
    }

    @Bean
    public Queue auditDlq() {
        return QueueBuilder.durable(auditDlqName).build();
    }

    // ==========================================
    // COLA 2 (Topic): notify.queue con su DLQ
    // ==========================================
    @Bean
    public TopicExchange notifyExchange() {
        return new TopicExchange(notifyExchangeName);
    }

    @Bean
    public Queue notifyQueue() {
        return QueueBuilder.durable(notifyQueueName)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .build();
    }

    @Bean
    public Binding notifyBinding(Queue notifyQueue, TopicExchange notifyExchange) {
        return BindingBuilder.bind(notifyQueue).to(notifyExchange).with(notifyRoutingKey);
    }

    @Bean
    public Queue notifyDlq() {
        return QueueBuilder.durable(notifyDlqName).build();
    }

    // ==========================================
    // COLA 3 (Direct): direct.queue con su DLQ
    // ==========================================
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(directExchangeName);
    }

    @Bean
    public Queue directQueue() {
        return QueueBuilder.durable(directQueueName)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .build();
    }

    @Bean
    public Binding directBinding(Queue directQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(directQueue).to(directExchange).with(directRoutingKey);
    }

    @Bean
    public Queue directDlq() {
        return QueueBuilder.durable(directDlqName).build();
    }

    // ==========================================
    // Dead Letter Exchange (DLX) y Bindings de DLQs
    // ==========================================
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(dlxName);
    }

    @Bean
    public Binding auditDlqBinding(Queue auditDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(auditDlq).to(deadLetterExchange).with(dlqRoutingKey);
    }

    @Bean
    public Binding notifyDlqBinding(Queue notifyDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(notifyDlq).to(deadLetterExchange).with(dlqRoutingKey);
    }

    @Bean
    public Binding directDlqBinding(Queue directDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(directDlq).to(deadLetterExchange).with(dlqRoutingKey);
    }

    // Conversores JSON y RabbitTemplate
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
