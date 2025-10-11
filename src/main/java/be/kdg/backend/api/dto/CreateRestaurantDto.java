package be.kdg.backend.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRestaurantDto(
        @NotBlank String name
) {}
