package be.kdg.backend.application.tracking;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-model entry for the order tracking screen (US21).
 * Written by event handlers on AMQP messages. Not part of Order aggregate.
 */
public record OrderEventEntry(
        Long id,
        UUID orderId,
        String eventType,
        LocalDateTime occurredAt,
        String payloadJson
) {
    public record EventType(String name) {}
}