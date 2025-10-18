package be.kdg.sa.backend.api.dto;

// DTOs voor communicatie MET restaurant service
public record RestaurantStatusResponse(
        boolean isOpen,
        String status,
        String message
) {}

