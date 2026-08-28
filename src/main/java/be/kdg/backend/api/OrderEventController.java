package be.kdg.backend.api;

import be.kdg.backend.application.OrderAcceptanceService;
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
 * These publish AMQP events to the kdg.events exchange. Accept is guarded by
 * US11 (open now) + US14 (prep feasible before closing) via {@link OrderAcceptanceService}.
 */
@RestController
@RequestMapping("/api/restaurants/{restaurantId}/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderEventController {

    private final OutboundEventPublisher outboundEventPublisher;
    private final IRestaurantRepository restaurantRepository;
    private final PendingOrderStore pendingOrderStore;
    private final OrderAcceptanceService orderAcceptanceService;

    /**
     * Canonical lifecycle transition (mistake #16): PATCH /orders/{orderId}/status
     * body {status: ACCEPTED|REJECTED|READY_FOR_PICKUP, reason?, extra?}.
     */
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<Void> updateOrderStatus(
            @PathVariable UUID restaurantId,
            @PathVariable UUID orderId,
            @RequestBody OrderStatusUpdate body) {
        String status = body == null || body.status() == null ? "" : body.status().toUpperCase();
        switch (status) {
            case "ACCEPTED" -> acceptInternal(restaurantId, orderId, body.extra());
            case "REJECTED" -> rejectInternal(orderId, body.reason());
            case "READY_FOR_PICKUP" -> readyInternal(orderId, restaurantId);
            default -> throw new IllegalArgumentException(
                    "Unsupported status '" + status + "' — expected ACCEPTED|REJECTED|READY_FOR_PICKUP");
        }
        return ResponseEntity.noContent().build();
    }

    /** Body of PATCH /restaurants/{id}/orders/{orderId}/status. */
    public record OrderStatusUpdate(String status, String reason, Map<String, String> extra) {}


    private void acceptInternal(UUID restaurantId, UUID orderId, Map<String, String> body) {
        orderAcceptanceService.verifyCanAccept(restaurantId, LocalDateTime.now());

        String pickupAddress = resolvePickupAddress(restaurantId, body);
        String deliveryAddress = resolveDeliveryAddress(orderId, body);

        var event = new InboundEvents.OrderAcceptedEvent(
                orderId, restaurantId, pickupAddress, deliveryAddress, LocalDateTime.now());
        outboundEventPublisher.publishOrderAccepted(event);
        pendingOrderStore.remove(orderId.toString());
        log.info("Manual accept order {} pickup={} delivery={}", orderId, pickupAddress, deliveryAddress);
    }

    private void rejectInternal(UUID orderId, String reason) {
        var event = new InboundEvents.OrderRejectedEvent(orderId, reason, LocalDateTime.now());
        outboundEventPublisher.publishOrderRejected(event);
        pendingOrderStore.remove(orderId.toString());
        log.info("Manual reject order {} — published OrderRejected", orderId);
    }

    private void readyInternal(UUID orderId, UUID restaurantId) {
        var event = new InboundEvents.OrderReadyForPickupEvent(orderId, restaurantId, LocalDateTime.now());
        outboundEventPublisher.publishOrderReadyForPickup(event);
        log.info("Manual ready order {} — published OrderReadyForPickup", orderId);
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