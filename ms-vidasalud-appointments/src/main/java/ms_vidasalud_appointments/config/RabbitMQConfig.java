package ms_vidasalud_appointments.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SuppressWarnings("removal")
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.audit}")
    private String auditQueueName;

    @Value("${rabbitmq.exchange.audit}")
    private String auditExchangeName;

    @Value("${rabbitmq.routing.key.audit}")
    private String auditRoutingKey;

    @Value("${rabbitmq.dlx}")
    private String dlxName;

    @Value("${rabbitmq.dlq.audit}")
    private String auditDlqName;

    @Value("${rabbitmq.routing.key.dlq}")
    private String dlqRoutingKey;

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

    // DLQ configuration
    @Bean
    public Queue auditDlq() {
        return QueueBuilder.durable(auditDlqName).build();
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(dlxName);
    }

    @Bean
    public Binding auditDlqBinding(Queue auditDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(auditDlq).to(deadLetterExchange).with(dlqRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
