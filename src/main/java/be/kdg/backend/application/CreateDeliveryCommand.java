package be.kdg.backend.application;

import be.kdg.backend.domain.Address;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDeliveryCommand(
        @NotBlank String orderId,
        @NotNull Address pickupAddress,
        @NotNull Address deliveryAddress
) {
    public CreateDeliveryCommand {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
    }
}
