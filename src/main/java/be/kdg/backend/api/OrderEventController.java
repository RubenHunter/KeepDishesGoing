package be.kdg.backend.api;

import be.kdg.backend.application.messaging.InboundEvents;
import be.kdg.backend.application.messaging.OutboundEventPublisher;
import be.kdg.backend.application.messaging.PendingOrderStore;
import be.kdg.backend.domain.restaurant.IRestaurantRepository;
import be.kdg.backend.domain.restaurant.RestaurantId;
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
 * These publish AMQP events to the kdg.events exchange.
 */
@RestController
@RequestMapping("/api/restaurants/{restaurantId}/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderEventController {

    private final OutboundEventPublisher outboundEventPublisher;
    private final IRestaurantRepository restaurantRepository;
    private final PendingOrderStore pendingOrderStore;

    @PostMapping("/{orderId}/accept")
    public ResponseEntity<Void> acceptOrder(
            @PathVariable UUID restaurantId,
            @PathVariable UUID orderId,
            @RequestBody(required = false) Map<String, String> body) {

        String pickupAddress = resolvePickupAddress(restaurantId, body);
        String deliveryAddress = resolveDeliveryAddress(orderId, body);

        var event = new InboundEvents.OrderAcceptedEvent(
                orderId, restaurantId, pickupAddress, deliveryAddress, LocalDateTime.now());
        outboundEventPublisher.publishOrderAccepted(event);
        pendingOrderStore.remove(orderId.toString());
        log.info("Manual accept order {} pickup={} delivery={}", orderId, pickupAddress, deliveryAddress);
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
        pendingOrderStore.remove(orderId.toString());
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

    private String resolvePickupAddress(UUID restaurantId, Map<String, String> body) {
        if (body != null && body.containsKey("pickupAddress") && !body.get("pickupAddress").isBlank()) {
            return body.get("pickupAddress");
        }
        return restaurantRepository.getFullAddress(new RestaurantId(restaurantId))
                .filter(addr -> !addr.isBlank())
                .orElse("Pickup at restaurant");
    }

    private String resolveDeliveryAddress(UUID orderId, Map<String, String> body) {
        if (body != null && body.containsKey("deliveryAddress") && !body.get("deliveryAddress").isBlank()) {
            return body.get("deliveryAddress");
        }
        return pendingOrderStore.get(orderId.toString());
    }
}