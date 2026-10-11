package ms_vidasalud_notify.messaging;

import com.rabbitmq.client.Channel;
import ms_vidasalud_notify.dto.AuditoriaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RabbitMQConsumer {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConsumer.class);

    @RabbitListener(queues = "${rabbitmq.queue.notify}")
    public void consume(AuditoriaEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            log.info("Notificando cambio de estado para atención {}: de {} a {}", 
                    event.getAtencionId(), event.getEstadoAnterior(), event.getEstadoNuevo());
            
            // Simulación de envío de correo / SMS
            // emailService.sendNotification(event);
            
            // Manual ACK
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Error procesando notificación. Enviando a DLQ", e);
            // NACK: multiple=false, requeue=false (send to DLQ)
            channel.basicNack(tag, false, false);
        }
    }
}
