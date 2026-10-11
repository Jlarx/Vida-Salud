package ms_vidasalud_audit.messaging;

import com.rabbitmq.client.Channel;
import ms_vidasalud_audit.dto.AuditoriaEvent;
import ms_vidasalud_audit.entity.AuditoriaAtencion;
import ms_vidasalud_audit.repository.AuditoriaAtencionRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Component
public class RabbitMQConsumer {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConsumer.class);
    private final AuditoriaAtencionRepository repository;

    public RabbitMQConsumer(AuditoriaAtencionRepository repository) {
        this.repository = repository;
    }

    @RabbitListener(queues = "${rabbitmq.queue.audit}")
    public void consume(AuditoriaEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            AuditoriaAtencion auditoria = new AuditoriaAtencion();
            auditoria.setAtencionId(event.getAtencionId());
            auditoria.setUsuarioEmail(event.getUsuarioEmail());
            auditoria.setEstadoAnterior(event.getEstadoAnterior());
            auditoria.setEstadoNuevo(event.getEstadoNuevo());
            auditoria.setFechaCambio(event.getFechaCambio());
            
            repository.save(auditoria);
            log.info("Evento de auditoría guardado exitosamente: {}", event.getAtencionId());
            
            // Manual ACK
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Error procesando evento de auditoría. Enviando a DLQ", e);
            // NACK: multiple=false, requeue=false (send to DLQ)
            channel.basicNack(tag, false, false);
        }
    }
}
