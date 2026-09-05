package be.kdg.backend.infrastructure.messaging;

import be.kdg.backend.application.messaging.InboundEvents;
import be.kdg.backend.application.messaging.OutboundEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * AMQP adapter implementing the {@link OutboundEventPublisher} port.
 * Only class in restaurant-service allowed to touch {@link RabbitTemplate} (coding-mistakes #7, #10).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AmqpEventPublisher implements OutboundEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishOrderAccepted(InboundEvents.OrderAcceptedEvent event) {
        sendEvent("order.accepted", event);
    }

    @Override
    public void publishOrderRejected(InboundEvents.OrderRejectedEvent event) {
        sendEvent("order.rejected", event);
    }

    @Override
    public void publishOrderReadyForPickup(InboundEvents.OrderReadyForPickupEvent event) {
        sendEvent("order.ready_for_pickup", event);
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
            throw e;
        }
    }
}