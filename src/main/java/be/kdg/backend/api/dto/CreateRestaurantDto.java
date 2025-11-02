package be.kdg.backend.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateRestaurantDto(
        @NotBlank String name,
        String fullAddress,
        @Email String email,
        String openingHours,
        String logo
) {}
