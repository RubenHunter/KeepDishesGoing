package be.kdg.backend.application.messaging.handlers;

import be.kdg.backend.application.OrderService;
import be.kdg.backend.application.messaging.InboundEvents;
import be.kdg.backend.application.tracking.TrackingService;
import be.kdg.backend.infrastructure.messaging.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Consumes {@code order.ready_for_pickup} (publisher: restaurant-service). */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderReadyForPickupHandler {

    private final OrderService orderService;
    private final TrackingService trackingService;

    @RabbitListener(queues = RabbitConfig.Q_ORDER_READY)
    public void handle(InboundEvents.OrderReadyForPickupEvent event) {
        log.info("Consumed OrderReadyForPickup orderId={}", event.orderId());
        trackingService.recordEvent(event.orderId(), "ORDER_READY_FOR_PICKUP",
                "{\"orderId\":\"" + event.orderId() + "\"}");
        try {
            orderService.onOrderReadyForPickup(event.orderId());
        } catch (Exception ex) {
            log.warn("OrderReadyForPickup handling failed for {}: {}", event.orderId(), ex.getMessage());
        }
    }
}