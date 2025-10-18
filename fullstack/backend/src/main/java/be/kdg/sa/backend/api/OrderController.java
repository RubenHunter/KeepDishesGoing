package be.kdg.sa.backend.api;

import be.kdg.sa.backend.api.dto.ValidationResponse;
import be.kdg.sa.backend.application.OrderApplicationService;
import be.kdg.sa.backend.application.CreateOrderCommand;
import be.kdg.sa.backend.application.OrderValidationService;
import be.kdg.sa.backend.domain.Order.Order;
import be.kdg.sa.backend.domain.Order.OrderId;
import be.kdg.sa.backend.domain.Order.OrderItem;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderApplicationService orderService;

    @PostMapping("/{orderId}/validate")
    public ResponseEntity<ValidationResponse> validateOrder(@PathVariable String orderId) {
        OrderValidationService.ValidationResult result =
                orderService.validateOrderBeforeCheckout(OrderId.of(orderId));

        return ResponseEntity.ok(new ValidationResponse(
                result.isValid(),
                result.message(),
                orderId
        ));
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        CreateOrderCommand command = new CreateOrderCommand(
                request.customerId(),
                request.restaurantId(),
                request.deliveryAddress(),
                request.customerEmail(),
                request.items().stream()
                        .map(item -> new CreateOrderCommand.OrderItemCommand(
                                item.menuItemId(),
                                item.itemName(),
                                item.quantity(),
                                item.unitPrice()
                        ))
                        .toList()
        );

        OrderId orderId = orderService.createOrder(command);

        return ResponseEntity.created(URI.create("/api/orders/" + orderId.getValue()))
                .body(new OrderResponse(orderId.getValue(), "Order created successfully", "PENDING"));
    }

    // ✅ CRUCIAL ENDPOINT: Plaats order en bevries inhoud + prijs
    @PostMapping("/{orderId}/place")
    public ResponseEntity<OrderResponse> placeOrder(@PathVariable String orderId) {
        orderService.placeOrder(OrderId.of(orderId));

        return ResponseEntity.ok(new OrderResponse(
                orderId,
                "Order placed successfully. Content and prices are now frozen.",
                "PLACED"
        ));
    }

    @PostMapping("/{orderId}/place-validated")
    public ResponseEntity<OrderResponse> placeOrderWithValidation(@PathVariable String orderId) {
        OrderResponse response = orderService.placeOrderWithValidation(OrderId.of(orderId));
        return ResponseEntity.ok(response);
    }

    //Check of order gewijzigd kan worden
    @GetMapping("/{orderId}/modifiable")
    public ResponseEntity<OrderModifiableResponse> canModifyOrder(@PathVariable String orderId) {
        boolean canModify = orderService.canModifyOrder(OrderId.of(orderId));

        return ResponseEntity.ok(new OrderModifiableResponse(
                orderId,
                canModify,
                canModify ? "Order can be modified" : "Order is placed and cannot be modified"
        ));
    }

    // ✅ ENDPOINT: Haal bevroren order details op
    @GetMapping("/{orderId}/details")
    public ResponseEntity<OrderDetailsResponse> getOrderDetails(@PathVariable String orderId) {
        OrderApplicationService.OrderDetails details = orderService.getOrderDetails(OrderId.of(orderId));

        return ResponseEntity.ok(OrderDetailsResponse.fromDomain(details));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderId) {
        Order order = orderService.getOrder(OrderId.of(orderId));
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{orderId}/accept")
    public ResponseEntity<Void> acceptOrder(@PathVariable String orderId) {
        orderService.acceptOrder(OrderId.of(orderId));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orderId}/reject")
    public ResponseEntity<Void> rejectOrder(@PathVariable String orderId, @RequestBody RejectOrderRequest request) {
        orderService.rejectOrder(OrderId.of(orderId), request.reason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orderId}/ready-for-pickup")
    public ResponseEntity<Void> markAsReadyForPickup(@PathVariable String orderId) {
        orderService.markOrderAsReadyForPickup(OrderId.of(orderId));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable String orderId, @RequestBody CancelOrderRequest request) {
        orderService.cancelOrder(OrderId.of(orderId), request.reason());
        return ResponseEntity.ok().build();
    }

    // ✅ NIEUWE DTO's voor US18
    public record OrderModifiableResponse(
            String orderId,
            boolean modifiable,
            String message
    ) {}

    public record OrderDetailsResponse(
            String orderId,
            String customerId,
            String restaurantId,
            java.util.List<OrderItemResponse> items,
            double totalAmount,
            String currency,
            String status,
            String orderPlacedAt,
            boolean isPlaced
    ) {
        public static OrderDetailsResponse fromDomain(OrderApplicationService.OrderDetails details) {
            return new OrderDetailsResponse(
                    details.orderId().getValue(),
                    details.customerId().getValue(),
                    details.restaurantId().getValue(),
                    details.items().stream()
                            .map(OrderItemResponse::fromDomain)
                            .toList(),
                    details.totalAmount().getAmount().doubleValue(),
                    details.totalAmount().getCurrency(),
                    details.status().name(),
                    details.orderPlacedAt() != null ? details.orderPlacedAt().toString() : null,
                    details.isPlaced()
            );
        }
    }

    public record OrderItemResponse(
            String menuItemId,
            String itemName,
            int quantity,
            double unitPrice,
            double lineTotal,
            String currency
    ) {
        public static OrderItemResponse fromDomain(OrderItem item) {
            return new OrderItemResponse(
                    item.getMenuItemId().getValue(),
                    item.getItemName(),
                    item.getQuantity().getValue(),
                    item.getUnitPrice().getAmount().doubleValue(),
                    item.calculateLineTotal().getAmount().doubleValue(),
                    item.getUnitPrice().getCurrency()
            );
        }
    }

    // Bestaande DTO's
    public record CreateOrderRequest(
            String customerId,
            String restaurantId,
            String deliveryAddress,
            String customerEmail,
            java.util.List<OrderItemRequest> items
    ) {}

    public record OrderItemRequest(
            String menuItemId,
            String itemName,
            int quantity,
            double unitPrice
    ) {}

    public record OrderResponse(String orderId, String message, String status) {}

    public record RejectOrderRequest(String reason) {}

    public record CancelOrderRequest(String reason) {}
}