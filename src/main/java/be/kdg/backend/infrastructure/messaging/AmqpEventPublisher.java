package be.kdg.backend.infrastructure.messaging;

import be.kdg.backend.application.messaging.InboundEvents;
import be.kdg.backend.application.messaging.OutboundEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AmqpEventPublisher implements OutboundEventPublisher {

    private final RabbitTemplate rabbit;

    @Override
    public void publishPickedUp(UUID orderId, UUID deliveryId, LocalDateTime at) {
        publish("order.picked_up", new InboundEvents.OrderPickedUpEvent(orderId, deliveryId, at));
    }

    @Override
    public void publishDelivered(UUID orderId, UUID deliveryId, LocalDateTime at) {
        publish("order.delivered", new InboundEvents.OrderDeliveredEvent(orderId, deliveryId, at));
    }

    private void publish(String routingKey, Object payload) {
        try {
            MessagePostProcessor mpp = m -> {
                m.getMessageProperties().setType(routingKey);
                return m;
            };
            rabbit.convertAndSend(RabbitConfig.EXCHANGE_NAME, routingKey, payload, mpp);
            log.info("Published {} to exchange={}", routingKey, RabbitConfig.EXCHANGE_NAME);
        } catch (Exception e) {
            log.error("Failed to publish AMQP event {}: {}", routingKey, e.getMessage(), e);
            throw e;
        }
    }
}