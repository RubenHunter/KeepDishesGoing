package be.kdg.backend.application.messaging.handlers;

import be.kdg.backend.application.messaging.EventPublisher;
import be.kdg.backend.application.messaging.InboundEvents;
import be.kdg.backend.application.messaging.OutboundEventPublisher;
import be.kdg.backend.infrastructure.messaging.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consumes {@code order.placed} (published by order-service). Per event catalog.
 *
 * Auto-accepts the order for demo flow — publishes {@code order.accepted} immediately.
 * In production this would run decision logic (US22/23/24: opening hours, feasibility)
 * and wait for the restaurant owner to accept/reject via an HTTP endpoint.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPlacedHandler {

    private final OutboundEventPublisher outboundEventPublisher;

    @RabbitListener(queues = RabbitConfig.Q_RESTAURANT_ORDER_EVENTS)
    public void handle(EventPublisher.OrderPlacedEvent event) {
        log.info("Consumed OrderPlaced orderId={} restaurantId={} items={}",
                event.orderId(), event.restaurantId(), event.items().size());

        try {
            var acceptedEvent = new InboundEvents.OrderAcceptedEvent(
                    UUID.fromString(event.orderId()),
                    UUID.fromString(event.restaurantId()),
                    "Pickup at restaurant",
                    LocalDateTime.now()
            );
            outboundEventPublisher.publishOrderAccepted(acceptedEvent);
            log.info("Auto-accepted order {} — published OrderAccepted", event.orderId());
        } catch (Exception e) {
            log.error("Failed to process OrderPlaced {}: {}", event.orderId(), e.getMessage(), e);
        }
    }
}