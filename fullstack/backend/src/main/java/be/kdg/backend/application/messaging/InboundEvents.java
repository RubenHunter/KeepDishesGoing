package be.kdg.backend.application.messaging;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Inbound event payload shapes consumed by order-service from RabbitMQ (per event catalog):
 *   - order.accepted (publisher: restaurant-service)
 *   - order.rejected (publisher: restaurant-service)
 *   - order.ready_for_pickup (publisher: restaurant-service)
 *   - order.picked_up (publisher: delivery-service)
 *   - order.delivered (publisher: delivery-service)
 *
 * These are local copies of the contract (microservice ownership). The producer side emits the
 * same shape — when shapes diverge, tests break.
 */
public final class InboundEvents {

    private InboundEvents() {}

    public record OrderAcceptedEvent(UUID orderId, UUID restaurantId, String pickupAddress, LocalDateTime acceptedAt) {
        public OrderAcceptedEvent { java.util.Objects.requireNonNull(orderId); }
    }

    public record OrderRejectedEvent(UUID orderId, String reason, LocalDateTime rejectedAt) {
        public OrderRejectedEvent { java.util.Objects.requireNonNull(orderId); }
    }

    public record OrderReadyForPickupEvent(UUID orderId, UUID restaurantId, LocalDateTime readyAt) {
        public OrderReadyForPickupEvent { java.util.Objects.requireNonNull(orderId); }
    }

    public record OrderPickedUpEvent(UUID orderId, UUID deliveryId, LocalDateTime pickedUpAt) {
        public OrderPickedUpEvent { java.util.Objects.requireNonNull(orderId); }
    }

    public record OrderDeliveredEvent(UUID orderId, UUID deliveryId, LocalDateTime deliveredAt) {
        public OrderDeliveredEvent { java.util.Objects.requireNonNull(orderId); }
    }
}