package be.kdg.backend.api.dto;

import java.time.LocalDateTime;

/**
 * Real-time status (US11/US13). {@code openNow} combines status (manual override, US12) + opening
 * hours; {@code closingTime}/{@code nextOpening} are populated when derivable from the schedule.
 */
public record RestaurantStatusResponse(
        boolean isOpen,
        String status,
        String message,
        boolean openNow,
        LocalDateTime closingTime,
        LocalDateTime nextOpening
) {}
