package be.kdg.backend.application.messaging.handlers;

import be.kdg.backend.application.OrderService;
import be.kdg.backend.application.messaging.InboundEvents;
import be.kdg.backend.application.tracking.TrackingService;
import be.kdg.backend.infrastructure.messaging.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Consumes {@code order.delivered} (publisher: delivery-service). Finalises order lifecycle. */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDeliveredHandler {

    private final OrderService orderService;
    private final TrackingService trackingService;

    @RabbitListener(queues = RabbitConfig.Q_ORDER_DELIVERED)
    public void handle(InboundEvents.OrderDeliveredEvent event) {
        log.info("Consumed OrderDelivered orderId={}", event.orderId());
        trackingService.recordEvent(event.orderId(), "ORDER_DELIVERED",
                "{\"orderId\":\"" + event.orderId() + "\",\"deliveryId\":\"" + event.deliveryId() + "\"}");
        try {
            orderService.onOrderDelivered(event.orderId());
        } catch (Exception ex) {
            log.warn("OrderDelivered handling failed for {}: {}", event.orderId(), ex.getMessage());
        }
    }
}