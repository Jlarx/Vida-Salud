package ms_vidasalud_report.messaging;

import com.rabbitmq.client.Channel;
import ms_vidasalud_report.dto.AuditoriaEvent;
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

    @RabbitListener(queues = "${rabbitmq.queue.direct:direct.queue}")
    public void consume(AuditoriaEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            log.info("[ms-vidasalud-report] Consolidando estadísticas de cita {}: estado actual {}", 
                    event.getAtencionId(), event.getEstadoNuevo());
            
            // Manual ACK
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("[ms-vidasalud-report] Error procesando estadísticas. Enviando a DLQ", e);
            channel.basicNack(tag, false, false);
        }
    }
}

