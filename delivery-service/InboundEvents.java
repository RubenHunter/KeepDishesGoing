package be.kdg.backend.application.messaging;

import java.time.LocalDateTime;
import java.util.UUID;

/** Inbound event payloads consumed by delivery-service from AMQP (per event catalog). */
public final class InboundEvents {
    private InboundEvents() {}

    /** publisher: restaurant-service — used to create a Delivery AR. */
    public record OrderAcceptedEvent(UUID orderId, UUID restaurantId, String pickupAddress, String deliveryAddress, LocalDateTime acceptedAt) {}

    /** publisher: restaurant-service — closes off US29 cancel window; US30 must-complete triggered. */
    public record OrderReadyForPickupEvent(UUID orderId, UUID restaurantId, LocalDateTime readyAt) {}

    /** publisher: delivery-service — consumed by order-service. FQCN matches order-service InboundEvents. */
    public record OrderPickedUpEvent(UUID orderId, UUID deliveryId, LocalDateTime pickedUpAt) {
        public OrderPickedUpEvent { java.util.Objects.requireNonNull(orderId); }
    }

    /** publisher: delivery-service — consumed by order-service. Finalises order lifecycle. */
    public record OrderDeliveredEvent(UUID orderId, UUID deliveryId, LocalDateTime deliveredAt) {
        public OrderDeliveredEvent { java.util.Objects.requireNonNull(orderId); }
    }
}