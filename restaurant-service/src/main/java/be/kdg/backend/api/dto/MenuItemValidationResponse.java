package be.kdg.backend.api.dto;

public record MenuItemValidationResponse(
        boolean isValid,
        String message,
        Double currentPrice,
        String currentCurrency,
        boolean isAvailable
) {}
