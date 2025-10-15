package be.kdg.sa.backend.api;

import be.kdg.sa.backend.application.CreateOrderCommand;
import be.kdg.sa.backend.application.OrderApplicationService;
import be.kdg.sa.backend.domain.Order.Order;
import be.kdg.sa.backend.domain.Order.OrderId;
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
                .body(new OrderResponse(orderId.getValue(), "Order created successfully"));
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

    public record OrderResponse(String orderId, String message) {}

    public record RejectOrderRequest(String reason) {}

    public record CancelOrderRequest(String reason) {}
}
