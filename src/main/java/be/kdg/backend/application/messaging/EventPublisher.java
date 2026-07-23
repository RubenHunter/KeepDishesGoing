package be.kdg.backend.application.messaging;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Inbound event payload consumed by restaurant-service from order-service via AMQP.
 *
 * FQCN must match order-service's EventPublisher so Jackson2JsonMessageConverter
 * can resolve the __TypeId__ header on the consumer side (microservice contract ownership).
 */
public interface EventPublisher {

    record OrderPlacedEvent(
            String orderId,
            String customerId,
            String restaurantId,
            List<OrderPlacedItem> items,
            LocalDateTime placedAt
    ) {
        public record OrderPlacedItem(String menuItemId, String itemName, int quantity, double unitPrice) {}
    }
}