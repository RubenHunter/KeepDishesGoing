package be.kdg.backend.application.messaging;

/**
 * Outbound event publisher port — owned by application, implemented in infrastructure (AMQP).
 * Domain never sees this. Application services publish events on aggregate transitions.
 */
public interface OutboundEventPublisher {
    void publishOrderAccepted(InboundEvents.OrderAcceptedEvent event);
    void publishOrderRejected(InboundEvents.OrderRejectedEvent event);
    void publishOrderReadyForPickup(InboundEvents.OrderReadyForPickupEvent event);
}