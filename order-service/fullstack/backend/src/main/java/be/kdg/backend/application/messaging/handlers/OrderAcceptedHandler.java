package be.kdg.backend.application.messaging.handlers;

import be.kdg.backend.application.OrderService;
import be.kdg.backend.application.messaging.InboundEvents;
import be.kdg.backend.application.tracking.TrackingService;
import be.kdg.backend.infrastructure.messaging.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code order.accepted} (published by restaurant-service). Per event catalog.
 *
 * Flow: AMQP → handler → TrackingService (read-model) + OrderService (aggregate update).
 * Each handler is its own transactional unit (one aggregate per tx rule).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAcceptedHandler {

    private final OrderService orderService;
    private final TrackingService trackingService;

    @RabbitListener(queues = RabbitConfig.Q_ORDER_ACCEPTED)
    public void handle(InboundEvents.OrderAcceptedEvent event) {
        log.info("Consumed OrderAccepted orderId={}", event.orderId());
        trackingService.recordEvent(event.orderId(), "ORDER_ACCEPTED", json(event));
        try {
            orderService.onOrderAccepted(event.orderId(), event.acceptedAt());
        } catch (Exception ex) {
            log.warn("OrderAccepted handling failed for {}: {}", event.orderId(), ex.getMessage());
        }
    }

    private String json(InboundEvents.OrderAcceptedEvent e) {
        return "{\"orderId\":\"" + e.orderId() + "\",\"restaurantId\":\"" + e.restaurantId()
                + "\",\"acceptedAt\":\"" + e.acceptedAt() + "\"}";
    }
}