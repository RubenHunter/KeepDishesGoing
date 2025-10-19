package be.kdg.backend.application;

import jakarta.validation.constraints.NotBlank;

public record AssignDeliveryPersonCommand(
        @NotBlank String deliveryId,
        @NotBlank String deliveryPersonId
) {
    public AssignDeliveryPersonCommand {
        if (deliveryId == null || deliveryId.isBlank()) {
            throw new IllegalArgumentException("Delivery ID cannot be null or empty");
        }
        if (deliveryPersonId == null || deliveryPersonId.isBlank()) {
            throw new IllegalArgumentException("Delivery Person ID cannot be null or empty");
        }
    }
}
