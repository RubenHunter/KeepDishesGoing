package be.kdg.backend.api;

import be.kdg.backend.application.messaging.InboundEvents;
import be.kdg.backend.application.messaging.OutboundEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Manual order event endpoints — lets the restaurant owner (or tester) trigger
 * order.accepted, order.rejected, order.ready_for_pickup events via HTTP.
 *
 * These publish AMQP events to the kdg.events exchange. Useful for testing
 * message flow in the RabbitMQ Management UI without running the full order flow.
 */
@RestController
@RequestMapping("/api/restaurants/{restaurantId}/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderEventController {

    private final OutboundEventPublisher outboundEventPublisher;

    @PostMapping("/{orderId}/accept")
    public ResponseEntity<Void> acceptOrder(
            @PathVariable UUID restaurantId,
            @PathVariable UUID orderId,
            @RequestBody(required = false) Map<String, String> body) {
        String pickupAddress = (body != null) ? body.getOrDefault("pickupAddress", "Pickup at restaurant") : "Pickup at restaurant";
        var event = new InboundEvents.OrderAcceptedEvent(orderId, restaurantId, pickupAddress, LocalDateTime.now());
        outboundEventPublisher.publishOrderAccepted(event);
        log.info("Manual accept order {} — published OrderAccepted", orderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{orderId}/reject")
    public ResponseEntity<Void> rejectOrder(
            @PathVariable UUID restaurantId,
            @PathVariable UUID orderId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = (body != null) ? body.getOrDefault("reason", "Restaurant rejected order") : "Restaurant rejected order";
        var event = new InboundEvents.OrderRejectedEvent(orderId, reason, LocalDateTime.now());
        outboundEventPublisher.publishOrderRejected(event);
        log.info("Manual reject order {} — published OrderRejected", orderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{orderId}/ready")
    public ResponseEntity<Void> markReady(
            @PathVariable UUID restaurantId,
            @PathVariable UUID orderId) {
        var event = new InboundEvents.OrderReadyForPickupEvent(orderId, restaurantId, LocalDateTime.now());
        outboundEventPublisher.publishOrderReadyForPickup(event);
        log.info("Manual ready order {} — published OrderReadyForPickup", orderId);
        return ResponseEntity.noContent().build();
    }
}