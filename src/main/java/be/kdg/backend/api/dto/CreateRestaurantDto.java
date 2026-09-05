package be.kdg.backend.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateRestaurantDto(
        @NotBlank String name,
        @NotBlank String fullAddress,
        @Email @NotBlank String email,
        @NotBlank String openingHours,
        @NotBlank String logo,
        String restaurantType
) {}
