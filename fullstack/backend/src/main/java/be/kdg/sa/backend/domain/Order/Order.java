package be.kdg.sa.backend.domain.Order;

import be.kdg.sa.backend.domain.Shared.Money;
import be.kdg.sa.backend.domain.Shared.Quantity;
import lombok.Getter;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@AggregateRoot
public class Order {
    @Identity
    @Getter
    private final OrderId id;

    @Getter
    private final CustomerId customerId;

    @Getter
    private final RestaurantId restaurantId;

    @Getter
    private final List<OrderItem> items;

    @Getter
    private OrderStatus status;

    @Getter
    private Money totalAmount;

    @Getter
    private final LocalDateTime createDate;

    @Getter
    private LocalDateTime updateDate;

    @Getter
    private final String deliveryAddress;

    @Getter
    private final String customerEmail;

    public Order(OrderId id, CustomerId customerId, RestaurantId restaurantId,
                 String deliveryAddress, String customerEmail) {
        if (id == null || customerId == null || restaurantId == null) {
            throw new IllegalArgumentException("Order ID, customer ID and restaurant ID cannot be null");
        }
        if (deliveryAddress == null || deliveryAddress.isBlank()) {
            throw new IllegalArgumentException("Delivery address cannot be null or empty");
        }
        if (customerEmail == null || customerEmail.isBlank()) {
            throw new IllegalArgumentException("Customer email cannot be null or empty");
        }

        this.id = id;
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.deliveryAddress = deliveryAddress;
        this.customerEmail = customerEmail;
        this.items = new ArrayList<>();
        this.status = OrderStatus.PENDING;
        this.totalAmount = Money.ZERO;
        this.createDate = LocalDateTime.now();
        this.updateDate = LocalDateTime.now();
    }

    private Order(OrderId id, CustomerId customerId, RestaurantId restaurantId,
                  String deliveryAddress, String customerEmail, OrderStatus status,
                  Money totalAmount, LocalDateTime createDate, LocalDateTime updateDate,
                  List<OrderItem> items) {
        this.id = id;
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.deliveryAddress = deliveryAddress;
        this.customerEmail = customerEmail;
        this.status = status;
        this.totalAmount = totalAmount;
        this.createDate = createDate;
        this.updateDate = updateDate;
        this.items = new ArrayList<>(items);
    }

    public static Order reconstruct(OrderId id, CustomerId customerId, RestaurantId restaurantId,
                                    String deliveryAddress, String customerEmail, OrderStatus status,
                                    Money totalAmount, LocalDateTime createDate, LocalDateTime updateDate,
                                    List<OrderItem> items) {
        return new Order(id, customerId, restaurantId, deliveryAddress, customerEmail,
                status, totalAmount, createDate, updateDate, items);
    }

    public void addItem(MenuItemId menuItemId, String itemName, Quantity quantity, Money unitPrice) {
        validateOrderInPendingState();

        if (menuItemId == null || itemName == null || quantity == null || unitPrice == null) {
            throw new IllegalArgumentException("MenuItem ID, item name, quantity and unit price cannot be null");
        }
        if (itemName.isBlank()) {
            throw new IllegalArgumentException("Item name cannot be blank");
        }

        OrderItem existingItem = items.stream()
                .filter(item -> item.isSameMenuItem(menuItemId))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            if (!existingItem.hasSamePrice(unitPrice)) {
                throw new IllegalArgumentException("Cannot add same menu item with different price");
            }
            existingItem.increaseQuantity(quantity.getValue());
        } else {
            OrderItem newItem = new OrderItem(menuItemId, itemName, quantity, unitPrice);
            items.add(newItem);
        }

        recalculateTotal();
        updateDate = LocalDateTime.now();
    }

    public void removeItem(MenuItemId menuItemId) {
        validateOrderInPendingState();

        if (menuItemId == null) {
            throw new IllegalArgumentException("MenuItem ID cannot be null");
        }

        boolean removed = items.removeIf(item -> item.getMenuItemId().equals(menuItemId));
        if (removed) {
            recalculateTotal();
            updateDate = LocalDateTime.now();
        }
    }

    public void updateItemQuantity(MenuItemId menuItemId, Quantity newQuantity) {
        validateOrderInPendingState();

        if (menuItemId == null || newQuantity == null) {
            throw new IllegalArgumentException("MenuItem ID and quantity cannot be null");
        }

        OrderItem item = items.stream()
                .filter(i -> i.getMenuItemId().equals(menuItemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item not found in order"));

        item.updateQuantity(newQuantity);
        recalculateTotal();
        updateDate = LocalDateTime.now();
    }

    public void placeOrder() {
        validateOrderInPendingState();
        validateOrderRules();

        this.status = OrderStatus.PLACED;
        this.updateDate = LocalDateTime.now();
    }

    public void acceptOrder() {
        if (this.status != OrderStatus.PLACED) {
            throw new IllegalStateException("Only PLACED orders can be accepted");
        }

        this.status = OrderStatus.ACCEPTED;
        this.updateDate = LocalDateTime.now();
    }

    public void rejectOrder(String reason) {
        if (this.status != OrderStatus.PLACED) {
            throw new IllegalStateException("Only PLACED orders can be rejected");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason cannot be null or empty");
        }

        this.status = OrderStatus.REJECTED;
        this.updateDate = LocalDateTime.now();
    }

    public void markAsReadyForPickup() {
        if (this.status != OrderStatus.ACCEPTED) {
            throw new IllegalStateException("Only ACCEPTED orders can be marked as ready for pickup");
        }

        this.status = OrderStatus.READY_FOR_PICKUP;
        this.updateDate = LocalDateTime.now();
    }

    public void cancelOrder(String reason) {
        if (this.status != OrderStatus.PLACED && this.status != OrderStatus.ACCEPTED) {
            throw new IllegalStateException("Only PLACED or ACCEPTED orders can be cancelled");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Cancellation reason cannot be null or empty");
        }

        this.status = OrderStatus.CANCELLED;
        this.updateDate = LocalDateTime.now();
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    private void validateOrderInPendingState() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Order modifications are only allowed in PENDING state");
        }
    }

    private void validateOrderRules() {
        if (items.isEmpty()) {
            throw new IllegalStateException("Order must have at least one item");
        }

        if (!totalAmount.isPositive()) {
            throw new IllegalStateException("Order total amount must be positive");
        }

        for (OrderItem item : items) {
            if (item.calculateLineTotal().isZero()) {
                throw new IllegalStateException("Order item line total cannot be zero");
            }
        }
    }

    private void recalculateTotal() {
        this.totalAmount = items.stream()
                .map(OrderItem::calculateLineTotal)
                .reduce(Money.ZERO, Money::add);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}