package be.kdg.backend.api.dto;

import be.kdg.backend.domain.delivery.Delivery;
import be.kdg.backend.domain.delivery.DeliveryStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryResponse(
        UUID deliveryId,
        UUID orderId,
        String pickupAddress,
        String deliveryAddress,
        UUID deliveryPersonId,
        DeliveryStatus status,
        LocalDateTime assignedAt,
        LocalDateTime readyAt,
        LocalDateTime pickedUpAt,
        LocalDateTime inTransitAt,
        LocalDateTime deliveredAt,
        LocalDateTime cancelledAt,
        String cancellationReason
) {
    public static DeliveryResponse from(Delivery d) {
        return new DeliveryResponse(
                d.id().value(),
                d.orderId().value(),
                d.pickupAddress().singleLine(),
                d.deliveryAddress().singleLine(),
                d.deliveryPersonId() == null ? null : d.deliveryPersonId().value(),
                d.status(),
                d.assignedAt(),
                d.readyAt(),
                d.pickedUpAt(),
                d.inTransitAt(),
                d.deliveredAt(),
                d.cancelledAt(),
                d.cancellationReason()
        );
    }
}