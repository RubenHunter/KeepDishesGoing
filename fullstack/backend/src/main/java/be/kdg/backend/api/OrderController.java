package be.kdg.backend.api;

import be.kdg.backend.api.dto.CheckoutRequest;
import be.kdg.backend.api.dto.CheckoutResponse;
import be.kdg.backend.api.dto.OrderStatusUpdateDto;
import be.kdg.backend.api.dto.TrackingResponse;
import be.kdg.backend.application.OrderService;
import be.kdg.backend.application.tracking.OrderEventEntry;
import be.kdg.backend.application.tracking.TrackingService;
import be.kdg.backend.domain.order.Order;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Order endpoints — customer-facing (authenticated) + owner console (role owner).
 * Customer identity is derived from the JWT subject, never from request bodies/paths.
 * Accept/reject lifecycle events are ingested ONLY through AMQP by handlers in
 * {@code application/messaging/handlers}.
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final TrackingService trackingService;

    /** Resource-style create — a checkout creates the order + payment session. */
    @PostMapping
    public ResponseEntity<CheckoutResponse> checkout(JwtAuthenticationToken auth,
                                                     @Valid @RequestBody CheckoutRequest req) {
        UUID customerId = UUID.fromString(auth.getToken().getSubject());
        OrderService.CheckoutResult result = orderService.checkout(
                req.cartId(), customerId, req.customerName(),
                req.street(), req.number(), req.postalCode(), req.city(), req.country(),
                req.email());
        return ResponseEntity.ok(new CheckoutResponse(
                result.orderId(), result.status(), result.paymentRef(), result.redirectUrl()));
    }

    /**
     * Canonical lifecycle transition (mistake #16): PATCH /orders/{orderId}/status
     * with body {status: PLACED|CANCELLED, reason?}.
     */
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<Void> updateOrderStatus(@PathVariable UUID orderId,
                                                  @Valid @RequestBody OrderStatusUpdateDto body) {
        switch (body.status().toUpperCase()) {
            case "PLACED" -> orderService.placeOrder(orderId);
            case "CANCELLED" -> orderService.cancelOrder(orderId,
                    body.reason() == null ? "Cancelled by customer" : body.reason());
            default -> throw new IllegalArgumentException(
                    "Unsupported status '" + body.status() + "' — expected PLACED|CANCELLED");
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetail> getOrder(@PathVariable UUID orderId) {
        Order o = orderService.loadOrder(orderId);
        return ResponseEntity.ok(OrderDetail.from(o));
    }

    /** US21 — GET order tracking screen (current status + ordered event history). */
    @GetMapping("/{orderId}/tracking")
    public ResponseEntity<TrackingResponse> tracking(@PathVariable UUID orderId) {
        Order o = orderService.loadOrder(orderId);
        List<OrderEventEntry> events = trackingService.eventsFor(orderId);
        return ResponseEntity.ok(new TrackingResponse(
                o.id().value(),
                o.status().name(),
                o.rejectReason(),
                o.placedAt(),
                o.acceptedAt(),
                o.readyAt(),
                o.pickedUpAt(),
                o.deliveredAt(),
                events.stream()
                        .map(e -> new TrackingResponse.TrackingEvent(e.eventType(), e.occurredAt(), e.payloadJson()))
                        .toList()
        ));
    }

    /**
     * Owner console — all orders for a restaurant, as a compact summary.
     * Lives in order-service because it owns the Order aggregate.
     */
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<OrderSummary>> listByRestaurant(@PathVariable UUID restaurantId) {
        List<Order> orders = orderService.listOrdersForRestaurant(restaurantId);
        return ResponseEntity.ok(orders.stream().map(OrderSummary::from).toList());
    }

    /**
     * Customer console — all orders for the current account, keyed by the Keycloak
     * subject so order history follows the account across devices.
     */
    @GetMapping("/customer")
    public ResponseEntity<List<OrderSummary>> listByCustomer(JwtAuthenticationToken auth) {
        UUID customerId = UUID.fromString(auth.getToken().getSubject());
        List<Order> orders = orderService.listOrdersForCustomer(customerId);
        return ResponseEntity.ok(orders.stream().map(OrderSummary::from).toList());
    }

    /** Compact summary for the owner/customer order list (avoid exposing full PII). */
    public record OrderSummary(
            UUID orderId,
            UUID restaurantId,
            String customerName,
            String status,
            double totalAmount,
            String currency,
            java.time.LocalDateTime placedAt,
            int itemCount,
            String deliveryAddress,
            java.util.List<OrderDetail.ItemDetail> items
    ) {
        public static OrderSummary from(Order o) {
            return new OrderSummary(
                    o.id().value(),
                    o.restaurantId().value(),
                    o.customerName(),
                    o.status().name(),
                    o.totalAmount().amount().doubleValue(),
                    o.totalAmount().currency(),
                    o.placedAt(),
                    o.items().size(),
                    o.deliveryAddress() == null ? null : o.deliveryAddress().singleLine(),
                    o.items().stream().map(it -> new OrderDetail.ItemDetail(
                            it.getMenuItemId().value().toString(),
                            it.getItemName(),
                            it.getQuantity().value(),
                            it.getUnitPrice().amount().doubleValue())).toList()
            );
        }
    }

    public record OrderDetail(
            UUID orderId,
            String customerId,
            String restaurantId,
            String customerName,
            String deliveryAddress,
            String customerEmail,
            double totalAmount,
            String currency,
            String status,
            String paymentRef,
            String paymentStatus,
            java.util.List<ItemDetail> items
    ) {
        public static OrderDetail from(Order o) {
            return new OrderDetail(
                    o.id().value(),
                    o.customerId().value().toString(),
                    o.restaurantId().value().toString(),
                    o.customerName(),
                    o.deliveryAddress() == null ? null : o.deliveryAddress().singleLine(),
                    o.customerEmail().value(),
                    o.totalAmount().amount().doubleValue(),
                    o.totalAmount().currency(),
                    o.status().name(),
                    o.paymentRef(),
                    o.paymentStatus() == null ? null : o.paymentStatus().name(),
                    o.items().stream().map(it -> new ItemDetail(
                            it.getMenuItemId().value().toString(),
                            it.getItemName(),
                            it.getQuantity().value(),
                            it.getUnitPrice().amount().doubleValue())).toList()
            );
        }

        public record ItemDetail(String menuItemId, String itemName, int quantity, double unitPrice) {}
    }
}