package be.kdg.backend.api.dto;

public record RestaurantStatusResponse(
        boolean isOpen,
        String status,
        String message
) {}