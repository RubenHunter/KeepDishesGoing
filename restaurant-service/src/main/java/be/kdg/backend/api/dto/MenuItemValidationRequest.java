package be.kdg.backend.api.dto;

public record MenuItemValidationRequest(
        double expectedPrice,
        String currency
) {}