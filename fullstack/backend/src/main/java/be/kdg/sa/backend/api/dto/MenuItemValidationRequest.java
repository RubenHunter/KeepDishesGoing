package be.kdg.sa.backend.api.dto;

public record MenuItemValidationRequest(
        double expectedPrice,
        String currency
) {}