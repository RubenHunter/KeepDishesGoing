package be.kdg.sa.backend.application;

import be.kdg.sa.backend.api.OrderController;
import be.kdg.sa.backend.domain.Order.*;
import be.kdg.sa.backend.domain.OrderRepository;
import be.kdg.sa.backend.domain.Shared.Money;
import be.kdg.sa.backend.domain.Shared.Quantity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OrderApplicationService {
    private final OrderRepository orderRepository;
    private final OrderValidationService orderValidationService;

    public OrderValidationService.ValidationResult validateOrderBeforeCheckout(OrderId orderId) {
        log.info("Validating order {} before checkout", orderId.getValue());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId.getValue()));

        return orderValidationService.validateOrderBeforeCheckout(order);
    }

    public OrderController.OrderResponse placeOrderWithValidation(OrderId orderId) {
        log.info("Placing order with validation: {}", orderId.getValue());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId.getValue()));

        // Valideer voor plaatsing
        OrderValidationService.ValidationResult validation =
                orderValidationService.validateOrderBeforeCheckout(order);

        if (!validation.isValid()) {
            log.warn("Order validation failed for {}: {}", orderId.getValue(), validation.message());
            throw new OrderValidationException("Order validation failed: " + validation.message());
        }

        // Plaats order als validatie slaagt
        order.placeOrder();
        Order savedOrder = orderRepository.save(order);

        log.info("Order placed successfully after validation: {}", orderId.getValue());

        return new OrderController.OrderResponse(
                savedOrder.getId().getValue(),
                "Order placed successfully after validation",
                savedOrder.getStatus().name()
        );
    }
    public OrderId createOrder(CreateOrderCommand command) {
        log.info("Creating order for customer: {}, restaurant: {}",
                command.customerId(), command.restaurantId());

        Order order = new Order(
                OrderId.generate(),
                CustomerId.of(command.customerId()),
                RestaurantId.of(command.restaurantId()),
                command.deliveryAddress(),
                command.customerEmail()
        );

        for (CreateOrderCommand.OrderItemCommand item : command.items()) {
            order.addItem(
                    MenuItemId.of(item.menuItemId()),
                    item.itemName(),
                    Quantity.of(item.quantity()),
                    Money.ofEuros(item.unitPrice())
            );
        }

        Order savedOrder = orderRepository.save(order);

        log.info("Order created successfully: {}", savedOrder.getId().getValue());
        return savedOrder.getId();
    }

    public void placeOrder(OrderId orderId) {
        log.info("Placing order: {}", orderId.getValue());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId.getValue()));

        order.placeOrder();

        Order savedOrder = orderRepository.save(order);

        log.info("Order placed successfully: {}. Total amount frozen: {} {}",
                orderId.getValue(),
                savedOrder.getTotalAmount().getAmount(),
                savedOrder.getTotalAmount().getCurrency());
    }

    public boolean canModifyOrder(OrderId orderId) {
        return orderRepository.findById(orderId)
                .map(order -> !order.isPlaced())
                .orElse(false);
    }

    public OrderDetails getOrderDetails(OrderId orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId.getValue()));

        return new OrderDetails(
                order.getId(),
                order.getCustomerId(),
                order.getRestaurantId(),
                order.getItems(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getOrderPlacedAt(),
                order.isPlaced()
        );
    }

    public void acceptOrder(OrderId orderId) {
        log.info("Accepting order: {}", orderId.getValue());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId.getValue()));

        order.acceptOrder();
        orderRepository.save(order);

        log.info("Order accepted: {}", orderId.getValue());
    }

    public void rejectOrder(OrderId orderId, String reason) {
        log.info("Rejecting order: {} with reason: {}", orderId.getValue(), reason);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId.getValue()));

        order.rejectOrder(reason);
        orderRepository.save(order);

        log.info("Order rejected: {}", orderId.getValue());
    }

    public void markOrderAsReadyForPickup(OrderId orderId) {
        log.info("Marking order as ready for pickup: {}", orderId.getValue());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId.getValue()));

        order.markAsReadyForPickup();
        orderRepository.save(order);

        log.info("Order marked as ready for pickup: {}", orderId.getValue());
    }

    public void cancelOrder(OrderId orderId, String reason) {
        log.info("Cancelling order: {} with reason: {}", orderId.getValue(), reason);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId.getValue()));

        order.cancelOrder(reason);
        orderRepository.save(order);

        log.info("Order cancelled: {}", orderId.getValue());
    }

    public Order getOrder(OrderId orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId.getValue()));
    }

    public record OrderDetails(
            OrderId orderId,
            CustomerId customerId,
            RestaurantId restaurantId,
            java.util.List<OrderItem> items,
            Money totalAmount,
            OrderStatus status,
            LocalDateTime orderPlacedAt,
            boolean isPlaced
    ) {}
}