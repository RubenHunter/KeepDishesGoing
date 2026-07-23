package be.kdg.backend.application.messaging.handlers;

import be.kdg.backend.application.DeliveryService;
import be.kdg.backend.application.messaging.InboundEvents;
import be.kdg.backend.domain.shared.OrderId;
import be.kdg.backend.infrastructure.messaging.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Consumes {@code order.ready_for_pickup} (publisher: restaurant-service). Locks courier cancel window (US29). */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderReadyForPickupHandler {

    private final DeliveryService deliveryService;

    @RabbitListener(queues = RabbitConfig.Q_DELIVERY_ORDER_READY)
    public void handle(InboundEvents.OrderReadyForPickupEvent event) {
        log.info("Consumed OrderReadyForPickup orderId={}", event.orderId());
        try {
            deliveryService.onOrderReadyForPickup(OrderId.of(event.orderId()), event.readyAt());
        } catch (Exception ex) {
            log.warn("OrderReadyForPickup handling failed for {}: {}", event.orderId(), ex.getMessage());
        }
    }
}