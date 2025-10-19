package be.kdg.backend.application;

import jakarta.validation.constraints.NotBlank;

public record ListAvailableDeliveriesCommand(
        @NotBlank String deliveryPersonId,
        Double latitude,
        Double longitude,
        Double maxRadiusKm
) {
    public ListAvailableDeliveriesCommand {
        if (deliveryPersonId == null || deliveryPersonId.isBlank()) {
            throw new IllegalArgumentException("Delivery Person ID cannot be null or empty");
        }
    }
}
