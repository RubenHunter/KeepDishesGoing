package be.kdg.backend.application.messaging;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Outbound event payloads published BY restaurant-service to AMQP.
 *
 * FQCN must match order-service's (and delivery-service's) InboundEvents so Jackson2JsonMessageConverter
 * can resolve the __TypeId__ header on the consumer side (microservice contract ownership).
 *
 * Event catalog:
 *   order.accepted          → consumed by order-service, delivery-service
 *   order.rejected          → consumed by order-service
 *   order.ready_for_pickup  → consumed by order-service, delivery-service
 */
public final class InboundEvents {

    private InboundEvents() {}

    public record OrderAcceptedEvent(UUID orderId, UUID restaurantId, String pickupAddress, String deliveryAddress, LocalDateTime acceptedAt) {
        public OrderAcceptedEvent { Objects.requireNonNull(orderId); }
    }

    public record OrderRejectedEvent(UUID orderId, String reason, LocalDateTime rejectedAt) {
        public OrderRejectedEvent { Objects.requireNonNull(orderId); }
    }

    public record OrderReadyForPickupEvent(UUID orderId, UUID restaurantId, LocalDateTime readyAt) {
        public OrderReadyForPickupEvent { Objects.requireNonNull(orderId); }
    }
}