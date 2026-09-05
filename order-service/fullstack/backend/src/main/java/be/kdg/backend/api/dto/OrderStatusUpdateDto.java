package be.kdg.backend.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body for PATCH /api/orders/{orderId}/status — one endpoint for the order
 * lifecycle transitions driven by the customer (mistake #16):
 * PLACED (US18, after payment PAID) or CANCELLED with a reason.
 */
public record OrderStatusUpdateDto(@NotBlank String status, String reason) {}
