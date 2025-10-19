package be.kdg.backend.application;

import jakarta.validation.constraints.NotBlank;

public record CancelDeliveryCommand(
        @NotBlank String deliveryId,
        @NotBlank String reason
) {
    public CancelDeliveryCommand {
        if (deliveryId == null || deliveryId.isBlank()) {
            throw new IllegalArgumentException("Delivery ID cannot be null or empty");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Cancellation reason cannot be null or empty");
        }
    }
}