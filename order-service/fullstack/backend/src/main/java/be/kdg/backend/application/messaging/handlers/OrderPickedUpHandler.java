package be.kdg.backend.application.messaging.handlers;

import be.kdg.backend.application.OrderService;
import be.kdg.backend.application.messaging.InboundEvents;
import be.kdg.backend.application.tracking.TrackingService;
import be.kdg.backend.infrastructure.messaging.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Consumes {@code order.picked_up} (publisher: delivery-service). */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPickedUpHandler {

    private final OrderService orderService;
    private final TrackingService trackingService;

    @RabbitListener(queues = RabbitConfig.Q_ORDER_PICKED_UP)
    public void handle(InboundEvents.OrderPickedUpEvent event) {
        log.info("Consumed OrderPickedUp orderId={}", event.orderId());
        trackingService.recordEvent(event.orderId(), "ORDER_PICKED_UP",
                "{\"orderId\":\"" + event.orderId() + "\",\"deliveryId\":\"" + event.deliveryId() + "\"}");
        try {
            orderService.onOrderPickedUp(event.orderId());
        } catch (Exception ex) {
            log.warn("OrderPickedUp handling failed for {}: {}", event.orderId(), ex.getMessage());
        }
    }
}