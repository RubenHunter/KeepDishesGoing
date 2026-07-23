package be.kdg.backend.application.messaging;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AMQP events published BY delivery-service.
 *  - order.picked_up    (consumed by order-service)
 *  - order.delivered     (consumed by order-service)
 */
public interface OutboundEventPublisher {
    void publishPickedUp(UUID orderId, UUID deliveryId, LocalDateTime at);
    void publishDelivered(UUID orderId, UUID deliveryId, LocalDateTime at);
}