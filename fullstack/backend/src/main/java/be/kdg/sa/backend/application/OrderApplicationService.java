package be.kdg.sa.backend.application;


import be.kdg.sa.backend.domain.Entities.MenuItemId;
import be.kdg.sa.backend.domain.Enums.*;
import be.kdg.sa.backend.domain.Money;
import be.kdg.sa.backend.domain.Order;
import be.kdg.sa.backend.domain.ValueObjects.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OrderApplicationService {
    private final OrderRepository orderRepository;

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

        order.placeOrder();
        Order savedOrder = orderRepository.save(order);

        log.info("Order created successfully: {}", savedOrder.getId().getValue());
        return savedOrder.getId();
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

    public void checkAndAutoRejectPendingOrders() {
        log.info("Checking for orders that need auto-rejection");
        // Implementation for US23-24: Auto-reject after 5 minutes
    }
}
