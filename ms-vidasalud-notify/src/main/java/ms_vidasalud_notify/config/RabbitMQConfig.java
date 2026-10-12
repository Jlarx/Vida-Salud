package ms_vidasalud_notify.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.notify}")
    private String notifyQueueName;

    @Value("${rabbitmq.exchange.notify}")
    private String notifyExchangeName;

    @Value("${rabbitmq.routing.key.notify}")
    private String notifyRoutingKey;

    @Value("${rabbitmq.dlx}")
    private String dlxName;

    @Value("${rabbitmq.dlq.notify}")
    private String notifyDlqName;

    @Value("${rabbitmq.routing.key.dlq}")
    private String dlqRoutingKey;

    @Bean
    public Queue notifyQueue() {
        return QueueBuilder.durable(notifyQueueName)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .build();
    }

    @Bean
    public TopicExchange notifyExchange() {
        return new TopicExchange(notifyExchangeName);
    }

    @Bean
    public Binding notifyBinding(Queue notifyQueue, TopicExchange notifyExchange) {
        return BindingBuilder.bind(notifyQueue).to(notifyExchange).with(notifyRoutingKey);
    }

    @Bean
    public Queue notifyDlq() {
        return QueueBuilder.durable(notifyDlqName).build();
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(dlxName);
    }

    @Bean
    public Binding notifyDlqBinding(Queue notifyDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(notifyDlq).to(deadLetterExchange).with(dlqRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
