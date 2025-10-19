package be.kdg.backend.application;

import be.kdg.backend.domain.Address;
import be.kdg.backend.domain.Delivery;

import java.time.LocalDateTime;

public record DeliveryResponse(
        String deliveryId,
        String orderId,
        String deliveryPersonId,
        Address pickupAddress,
        Address deliveryAddress,
        String status,
        LocalDateTime estimatedDeliveryTime,
        LocalDateTime assignedAt,
        LocalDateTime pickedUpAt,
        LocalDateTime deliveredAt,
        String cancellationReason
) {
    public static DeliveryResponse fromDomain(Delivery delivery) {
        return new DeliveryResponse(
                delivery.getId().value(),
                delivery.getOrderId().value(),
                delivery.getDeliveryPersonId() != null ? delivery.getDeliveryPersonId().value() : null,
                delivery.getPickupAddress(),
                delivery.getDeliveryAddress(),
                delivery.getStatus().name(),
                delivery.getEstimatedDeliveryTime(),
                delivery.getAssignedAt(),
                delivery.getPickedUpAt(),
                delivery.getDeliveredAt(),
                delivery.getCancellationReason() != null ? delivery.getCancellationReason().value() : null
        );
    }
}