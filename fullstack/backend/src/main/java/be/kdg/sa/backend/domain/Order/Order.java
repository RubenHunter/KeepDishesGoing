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

    private final List<OrderItem> items;

    @Getter
    private OrderStatus status;

    private Money totalAmount; // ✅ Veranderd naar niet-final

    @Getter
    private final LocalDateTime createDate;

    @Getter
    private LocalDateTime updateDate;

    @Getter
    private final String deliveryAddress;

    @Getter
    private final String customerEmail;

    @Getter
    private LocalDateTime orderPlacedAt; // ✅ Veranderd naar niet-final

    public Order(OrderId id, CustomerId customerId, RestaurantId restaurantId,
                 String deliveryAddress, String customerEmail) {
        validateConstructorParameters(id, customerId, restaurantId, deliveryAddress, customerEmail);

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
        this.orderPlacedAt = null;
    }

    private Order(OrderId id, CustomerId customerId, RestaurantId restaurantId,
                  String deliveryAddress, String customerEmail, OrderStatus status,
                  Money totalAmount, LocalDateTime createDate, LocalDateTime updateDate,
                  LocalDateTime orderPlacedAt, List<OrderItem> items) {
        this.id = id;
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.deliveryAddress = deliveryAddress;
        this.customerEmail = customerEmail;
        this.status = status;
        this.totalAmount = totalAmount;
        this.createDate = createDate;
        this.updateDate = updateDate;
        this.orderPlacedAt = orderPlacedAt;
        this.items = new ArrayList<>(items);
    }

    public static Order reconstruct(OrderId id, CustomerId customerId, RestaurantId restaurantId,
                                    String deliveryAddress, String customerEmail, OrderStatus status,
                                    Money totalAmount, LocalDateTime createDate, LocalDateTime updateDate,
                                    LocalDateTime orderPlacedAt, List<OrderItem> items) {
        Order order = new Order(id, customerId, restaurantId, deliveryAddress, customerEmail,
                status, totalAmount, createDate, updateDate, orderPlacedAt, items);
        order.validateOrderConsistency();
        return order;
    }

    public void placeOrder() {
        validateOrderInPendingState();
        validateOrderRules();

        // ✅ BEVRIES INHOUD EN PRIJS - sla het huidige totaal op
        this.totalAmount = calculateDynamicTotal();
        this.status = OrderStatus.PLACED;
        this.orderPlacedAt = LocalDateTime.now();
        this.updateDate = LocalDateTime.now();

        validateFrozenOrderState();
    }

    public void addItem(MenuItemId menuItemId, String itemName, Quantity quantity, Money unitPrice) {
        validateOrderInPendingState();

        if (menuItemId == null || itemName == null || quantity == null || unitPrice == null) {
            throw new IllegalArgumentException("MenuItem ID, item name, quantity and unit price cannot be null");
        }
        if (itemName.isBlank()) {
            throw new IllegalArgumentException("Item name cannot be blank");
        }

        OrderItem existingItem = findCartItem(menuItemId);
        if (existingItem != null) {
            if (!existingItem.hasSamePrice(unitPrice)) {
                throw new IllegalArgumentException("Cannot add same menu item with different price");
            }
            existingItem.increaseQuantity(quantity);
        } else {
            OrderItem newItem = OrderItem.create(menuItemId, itemName, quantity, unitPrice);
            items.add(newItem);
        }

        updateTimestamp();
    }

    public void removeItem(MenuItemId menuItemId) {
        validateOrderInPendingState();

        if (menuItemId == null) {
            throw new IllegalArgumentException("MenuItem ID cannot be null");
        }

        boolean removed = items.removeIf(item -> item.getMenuItemId().equals(menuItemId));
        if (removed) {
            updateTimestamp();
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
        updateTimestamp();
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

    public Money getTotalAmount() {
        if (status == OrderStatus.PENDING) {
            return calculateDynamicTotal();
        } else {
            return totalAmount;
        }
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public boolean isPlaced() {
        return status != OrderStatus.PENDING;
    }

    private void validateOrderInPendingState() {
        if (this.status != OrderStatus.PENDING) {
            throw new OrderFrozenException(
                    "Order modifications are not allowed after order is placed. Current status: " + this.status
            );
        }
    }

    private void validateOrderRules() {
        if (items.isEmpty()) {
            throw new IllegalStateException("Order must have at least one item");
        }

        Money calculatedTotal = calculateDynamicTotal();
        if (!calculatedTotal.isPositive()) {
            throw new IllegalStateException("Order total amount must be positive");
        }

        for (OrderItem item : items) {
            if (item.calculateLineTotal().isZero()) {
                throw new IllegalStateException("Order item line total cannot be zero");
            }
        }
    }

    private void validateFrozenOrderState() {
        if (orderPlacedAt == null) {
            throw new IllegalStateException("Order placed timestamp must be set when order is placed");
        }

        // ✅ CORRECTIE: Vergelijk met de opgeslagen totalAmount, niet met dynamische berekening
        Money calculatedTotal = calculateDynamicTotal();
        if (!calculatedTotal.equals(totalAmount)) {
            throw new IllegalStateException(
                    String.format("Order total amount is inconsistent with items after placement. Calculated: %.2f %s, Stored: %.2f %s",
                            calculatedTotal.getAmount().doubleValue(), calculatedTotal.getCurrency(),
                            totalAmount.getAmount().doubleValue(), totalAmount.getCurrency())
            );
        }
    }

    private void validateOrderConsistency() {
        if (status != OrderStatus.PENDING && orderPlacedAt == null) {
            throw new IllegalStateException("Non-pending orders must have order placed timestamp");
        }

        if (status != OrderStatus.PENDING && !totalAmount.equals(calculateDynamicTotal())) {
            throw new IllegalStateException("Order total amount is inconsistent with items");
        }
    }

    private void validateConstructorParameters(OrderId id, CustomerId customerId, RestaurantId restaurantId,
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
    }

    private Money calculateDynamicTotal() {
        return items.stream()
                .map(OrderItem::calculateLineTotal)
                .reduce(Money.ZERO, Money::add);
    }

    private OrderItem findCartItem(MenuItemId menuItemId) {
        return items.stream()
                .filter(item -> item.getMenuItemId().equals(menuItemId))
                .findFirst()
                .orElse(null);
    }

    private void updateTimestamp() {
        this.updateDate = LocalDateTime.now();
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