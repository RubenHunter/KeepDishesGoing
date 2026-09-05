package be.kdg.backend.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddCartItemRequest(
        @NotNull UUID menuItemId,
        @NotBlank String itemName,
        @NotNull @Min(1) Integer quantity,
        @NotNull Double unitPrice,
        @NotNull UUID restaurantId
) {}