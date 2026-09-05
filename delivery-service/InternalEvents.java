package be.kdg.backend.application.messaging;

import be.kdg.backend.domain.shared.DeliveryId;
import be.kdg.backend.domain.shared.DeliveryPersonId;

import java.time.LocalDateTime;

/**
 * Spring application events internal to delivery-service — used to enforce one-aggregate-per-tx:
 * - save Delivery in tx1 (publishes this after commit),
 * - save DeliveryPerson in tx2 (listener).
 *
 * One-Pager IAO topic: Spring ApplicationEvents to decouple aggregates inside one service.
 */
public final class InternalEvents {
    private InternalEvents() {}

    public record CourierAssignedEvent(DeliveryId deliveryId, DeliveryPersonId driverId, LocalDateTime at) {}

    public record CourierReleasedEvent(DeliveryPersonId driverId, LocalDateTime at) {}

    public record DeliveryDeliveredEvent(
            DeliveryId deliveryId,
            DeliveryPersonId driverId,
            LocalDateTime readyAt,
            LocalDateTime deliveredAt) {}
}