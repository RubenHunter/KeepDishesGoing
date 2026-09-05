package be.kdg.backend.api.dto;

import java.util.UUID;

public record CheckoutResponse(
        UUID orderId,
        String status,
        String paymentRef,
        String redirectUrl
) {}