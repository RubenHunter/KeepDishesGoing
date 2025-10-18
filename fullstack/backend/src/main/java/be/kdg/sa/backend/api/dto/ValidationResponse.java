package be.kdg.sa.backend.api.dto;

public record ValidationResponse(
        boolean isValid,
        String message,
        String orderId
) {}
