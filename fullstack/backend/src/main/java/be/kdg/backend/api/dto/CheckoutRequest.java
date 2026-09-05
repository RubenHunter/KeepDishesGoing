package be.kdg.backend.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * US19: customer name, full delivery address, email captured at checkout.
 */
public record CheckoutRequest(
        UUID cartId,
        @NotBlank String customerName,
        @NotBlank String street,
        @NotBlank String number,
        @NotBlank String postalCode,
        @NotBlank String city,
        @NotBlank String country,
        @NotBlank @jakarta.validation.constraints.Email String email
) {}