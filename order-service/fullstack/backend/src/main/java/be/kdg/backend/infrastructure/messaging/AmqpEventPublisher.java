package be.kdg.backend.infrastructure.messaging;

import be.kdg.backend.application.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * AMQP adapter implementing the {@link EventPublisher} port.
 * Only class in the order-service allowed to touch {@link RabbitTemplate} (coding-mistakes #7, #10).
 * Logs errors and lets Spring retry configured via publisher confirms (rubric: "...uitgaande order topic
 * automatisch opnieuw verwerkt").
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AmqpEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishOrderPlaced(OrderPlacedEvent event) {
        sendEvent("order.placed", event);
    }

    @Override
    public void publishOrderCancelled(OrderCancelledEvent event) {
        sendEvent("order.cancelled", event);
    }

    private void sendEvent(String routingKey, Object event) {
        try {
            MessagePostProcessor mpp = m -> {
                m.getMessageProperties().setType(routingKey);
                return m;
            };
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, routingKey, event, mpp);
            log.info("Published {} to exchange={}", routingKey, RabbitConfig.EXCHANGE_NAME);
        } catch (Exception e) {
            log.error("Failed to publish AMQP event {}: {}", routingKey, e.getMessage(), e);
            // Re-throw — Spring AMQP will retry per publisher confirms.
            throw e;
        }
    }
}