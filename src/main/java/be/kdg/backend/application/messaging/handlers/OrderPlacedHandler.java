package be.kdg.backend.application.messaging.handlers;

import be.kdg.backend.application.messaging.EventPublisher;
import be.kdg.backend.application.messaging.PendingOrderStore;
import be.kdg.backend.infrastructure.messaging.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code order.placed} (published by order-service). Per event catalog.
 *
 * Per US22/23/24 the OWNER decides (accept/reject via the UI or HTTP endpoint).
 * The previous demo stub auto-accepted every order, which defeated US22/23/24 —
 * disabled: we only log the event here, the decision events come from
 * {@code OrderEventController} (owner action) instead.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPlacedHandler {

    private final PendingOrderStore pendingOrderStore;

    @RabbitListener(queues = RabbitConfig.Q_RESTAURANT_ORDER_EVENTS)
    public void handle(EventPublisher.OrderPlacedEvent event) {
        log.info("Consumed OrderPlaced orderId={} restaurantId={} items={} — awaiting owner decision (US22)",
                event.orderId(), event.restaurantId(), event.items().size());
        if (event.deliveryAddress() != null && !event.deliveryAddress().isBlank()) {
            pendingOrderStore.put(event.orderId(), event.deliveryAddress());
        }
    }
}