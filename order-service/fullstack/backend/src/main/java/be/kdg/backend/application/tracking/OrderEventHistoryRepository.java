package be.kdg.backend.application.tracking;

import java.util.List;
import java.util.UUID;

/**
 * Read-model repository for order events (US21 tracking screen).
 * Used only by AMQP handlers and {@code GET /api/orders/{id}/tracking} — never by domain aggregates.
 */
public interface OrderEventHistoryRepository {
    OrderEventEntry save(OrderEventEntry entry);
    List<OrderEventEntry> findByOrderId(UUID orderId);
}