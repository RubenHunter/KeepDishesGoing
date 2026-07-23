package be.kdg.backend.application.messaging;

/**
 * Outbound event publisher port — owned by application, implemented in infrastructure (AMQP).
 * Domain never sees this. Application services publish events on aggregate transitions.
 */
public interface EventPublisher {
    void publishOrderPlaced(OrderPlacedEvent event);
    void publishOrderCancelled(OrderCancelledEvent event);

    record OrderPlacedEvent(
            String orderId,
            String customerId,
            String restaurantId,
            java.util.List<OrderPlacedItem> items,
            java.time.LocalDateTime placedAt
    ) {
        public record OrderPlacedItem(String menuItemId, String itemName, int quantity, double unitPrice) {}
    }

    record OrderCancelledEvent(
            String orderId,
            String reason,
            java.time.LocalDateTime cancelledAt
    ) {}
}