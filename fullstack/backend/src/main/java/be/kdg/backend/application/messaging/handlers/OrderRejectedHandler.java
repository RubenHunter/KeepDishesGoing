package be.kdg.backend.application.messaging.handlers;

import be.kdg.backend.application.OrderService;
import be.kdg.backend.application.messaging.InboundEvents;
import be.kdg.backend.application.tracking.TrackingService;
import be.kdg.backend.infrastructure.messaging.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Consumes {@code order.rejected} (publisher: restaurant-service). */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderRejectedHandler {

    private final OrderService orderService;
    private final TrackingService trackingService;

    @RabbitListener(queues = RabbitConfig.Q_ORDER_REJECTED)
    public void handle(InboundEvents.OrderRejectedEvent event) {
        log.info("Consumed OrderRejected orderId={} reason={}", event.orderId(), event.reason());
        trackingService.recordEvent(event.orderId(), "ORDER_REJECTED",
                "{\"orderId\":\"" + event.orderId() + "\",\"reason\":\"" + event.reason() + "\"}");
        try {
            orderService.onOrderRejected(event.orderId(), event.reason(), event.rejectedAt());
        } catch (Exception ex) {
            log.warn("OrderRejected handling failed for {}: {}", event.orderId(), ex.getMessage());
        }
    }
}