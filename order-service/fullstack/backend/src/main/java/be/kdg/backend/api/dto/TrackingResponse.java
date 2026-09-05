package be.kdg.backend.api.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** US21 tracking screen — order current state plus an ordered event history. */
public record TrackingResponse(
        UUID orderId,
        String status,
        String rejectReason,
        LocalDateTime placedAt,
        LocalDateTime acceptedAt,
        LocalDateTime readyAt,
        LocalDateTime pickedUpAt,
        LocalDateTime deliveredAt,
        List<TrackingEvent> events
) {
    public record TrackingEvent(String type, LocalDateTime occurredAt, String payloadJson) {}
}