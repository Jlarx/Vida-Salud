package ms_vidasalud_appointments.service;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.stereotype.Service;

@Service
public class RabbitAdminService {

    private final RabbitAdmin rabbitAdmin;

    public RabbitAdminService(ConnectionFactory connectionFactory) {
        this.rabbitAdmin = new RabbitAdmin(connectionFactory);
    }

    public void createQueue(String queueName) {
        rabbitAdmin.declareQueue(new Queue(queueName, true));
    }

    public void deleteQueue(String queueName) {
        rabbitAdmin.deleteQueue(queueName);
    }

    public void createExchange(String exchangeName) {
        rabbitAdmin.declareExchange(new TopicExchange(exchangeName));
    }

    public void createBinding(String queueName, String exchangeName, String routingKey) {
        rabbitAdmin.declareBinding(BindingBuilder
                .bind(new Queue(queueName, true))
                .to(new TopicExchange(exchangeName))
                .with(routingKey));
    }
}
