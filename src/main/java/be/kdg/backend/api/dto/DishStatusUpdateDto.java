package be.kdg.backend.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body for PATCH /restaurants/{id}/dishes/{dishId}/status — one endpoint for the
 * dish lifecycle transitions (mistake #16): PUBLISHED, DRAFT (de-publish), or
 * availability via {@code available} (US9, never schedulable).
 */
public record DishStatusUpdateDto(@NotBlank String status, Boolean available) {}
