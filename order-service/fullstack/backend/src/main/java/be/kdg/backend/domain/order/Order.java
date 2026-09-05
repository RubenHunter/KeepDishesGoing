package be.kdg.backend.domain.order;

import be.kdg.backend.domain.shared.Address;
import be.kdg.backend.domain.shared.CustomerId;
import be.kdg.backend.domain.shared.Email;
import be.kdg.backend.domain.shared.MenuItemId;
import be.kdg.backend.domain.shared.Money;
import be.kdg.backend.domain.shared.OrderSnapshot;
import be.kdg.backend.domain.shared.Quantity;
import be.kdg.backend.domain.shared.RestaurantId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Order aggregate root — owns its state machine + invariants.
 * Application services only call methods on this aggregate; they do NOT contain state logic.
 *
 * US18: contents and prices are frozen the moment {@link #place()} is called.
 * US21: tracking timestamps are recorded here; rendered via tracking read-model in app layer.
 */
@AggregateRoot
public class Order {

    @Identity
    private final OrderId id;
    private final CustomerId customerId;
    private final RestaurantId restaurantId;
    private final List<OrderItem> items;
    private final Money totalAmount;                  // frozen at PENDING→PLACED (or at construction if PLACED)
    private final String customerName;
    private final Address deliveryAddress;
    private final Email customerEmail;

    private OrderStatus status;
    private String rejectReason;
    private String paymentRef;
    private PaymentStatus paymentStatus;

    private final LocalDateTime createdAt;
    private LocalDateTime placedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime readyAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime updatedAt;

    // ---- Construction -----------------------------------------------------

    /** Constructor for new (PENDING) orders from a checkout snapshot. */
    public Order(OrderId id, CustomerId customerId, RestaurantId restaurantId,
                 String customerName, Address deliveryAddress, Email customerEmail,
                 OrderSnapshot snapshot) {
        requireNonNull(id, "id");
        requireNonNull(customerId, "customerId");
        requireNonNull(restaurantId, "restaurantId");
        requireNonBlank(customerName, "customerName");
        requireNonNull(deliveryAddress, "deliveryAddress");
        requireNonNull(customerEmail, "customerEmail");
        requireNonNull(snapshot, "snapshot");

        if (!restaurantId.equals(snapshot.restaurantId())) {
            throw new IllegalArgumentException("Restaurant id mismatch between order and snapshot");
        }
        if (snapshot.items().isEmpty()) {
            throw new IllegalArgumentException("Cannot create order from empty snapshot");
        }
        if (!snapshot.total().isPositive()) {
            throw new IllegalArgumentException("Order total must be positive");
        }

        this.id = id;
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.customerName = customerName;
        this.deliveryAddress = deliveryAddress;
        this.customerEmail = customerEmail;
        this.items = new ArrayList<>(snapshot.items().size());
        snapshot.items().forEach(s ->
                this.items.add(OrderItem.create(s.menuItemId(), s.itemName(), s.quantity(), s.unitPrice())));
        this.totalAmount = snapshot.total();
        this.status = OrderStatus.PENDING;
        this.paymentStatus = PaymentStatus.AWAITING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /** Rehydrate constructor (from persistence). */
    private Order(OrderId id, CustomerId customerId, RestaurantId restaurantId,
                  List<OrderItem> items, Money totalAmount, String customerName,
                  Address deliveryAddress, Email customerEmail, OrderStatus status,
                  String rejectReason, String paymentRef, PaymentStatus paymentStatus,
                  LocalDateTime createdAt, LocalDateTime placedAt, LocalDateTime acceptedAt,
                  LocalDateTime rejectedAt, LocalDateTime readyAt, LocalDateTime pickedUpAt,
                  LocalDateTime deliveredAt, LocalDateTime cancelledAt, LocalDateTime updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.items = new ArrayList<>(items);
        this.totalAmount = totalAmount;
        this.customerName = customerName;
        this.deliveryAddress = deliveryAddress;
        this.customerEmail = customerEmail;
        this.status = status;
        this.rejectReason = rejectReason;
        this.paymentRef = paymentRef;
        this.paymentStatus = paymentStatus;
        this.createdAt = createdAt;
        this.placedAt = placedAt;
        this.acceptedAt = acceptedAt;
        this.rejectedAt = rejectedAt;
        this.readyAt = readyAt;
        this.pickedUpAt = pickedUpAt;
        this.deliveredAt = deliveredAt;
        this.cancelledAt = cancelledAt;
        this.updatedAt = updatedAt;
        validateConsistency();
    }

    public static Order rehydrate(OrderId id, CustomerId customerId, RestaurantId restaurantId,
                                  List<OrderItem> items, Money totalAmount, String customerName,
                                  Address deliveryAddress, Email customerEmail, OrderStatus status,
                                  String rejectReason, String paymentRef, PaymentStatus paymentStatus,
                                  LocalDateTime createdAt, LocalDateTime placedAt, LocalDateTime acceptedAt,
                                  LocalDateTime rejectedAt, LocalDateTime readyAt, LocalDateTime pickedUpAt,
                                  LocalDateTime deliveredAt, LocalDateTime cancelledAt, LocalDateTime updatedAt) {
        return new Order(id, customerId, restaurantId, items, totalAmount, customerName,
                deliveryAddress, customerEmail, status, rejectReason, paymentRef, paymentStatus,
                createdAt, placedAt, acceptedAt, rejectedAt, readyAt, pickedUpAt,
                deliveredAt, cancelledAt, updatedAt);
    }

    // ---- State machine — each transition validates its precondition ----------

    public void place() {
        requirePending("place");
        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot place empty order");
        }
        if (!paymentStatus.equals(PaymentStatus.PAID)) {
            throw new IllegalStateException("Order must be paid before it can be placed");
        }
        this.status = OrderStatus.PLACED;
        this.placedAt = LocalDateTime.now();
        touch();
    }

    public void accept() {
        requireStatus(OrderStatus.PLACED, "accept");
        this.status = OrderStatus.ACCEPTED;
        this.acceptedAt = LocalDateTime.now();
        touch();
    }

    public void reject(String reason) {
        requireStatus(OrderStatus.PLACED, "reject");
        requireNonBlank(reason, "reason");
        this.status = OrderStatus.REJECTED;
        this.rejectReason = reason;
        this.rejectedAt = LocalDateTime.now();
        touch();
    }

    public void markReadyForPickup() {
        requireStatus(OrderStatus.ACCEPTED, "markReadyForPickup");
        this.status = OrderStatus.READY_FOR_PICKUP;
        this.readyAt = LocalDateTime.now();
        touch();
    }

    public void markPickedUp() {
        requireStatus(OrderStatus.READY_FOR_PICKUP, "markPickedUp");
        this.status = OrderStatus.PICKED_UP;
        this.pickedUpAt = LocalDateTime.now();
        touch();
    }

    public void markDelivered() {
        requireStatus(OrderStatus.PICKED_UP, "markDelivered");
        this.status = OrderStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
        touch();
    }

    public void cancel(String reason) {
        requireNonBlank(reason, "reason");
        if (!status.canCancelFrom()) {
            throw new OrderFrozenException(
                    "Cannot cancel order in status " + status);
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.rejectReason = reason; // reuse field for cancellation reason
        touch();
    }

    public void assignPayment(String paymentRef, PaymentStatus paymentStatus) {
        requirePending("assignPayment");
        requireNonBlank(paymentRef, "paymentRef");
        requireNonNull(paymentStatus, "paymentStatus");
        this.paymentRef = paymentRef;
        this.paymentStatus = paymentStatus;
        touch();
    }

    // ---- Queries -----------------------------------------------------------

    public boolean isModifiable() { return status == OrderStatus.PENDING; }
    public boolean isPlaced()    { return status != OrderStatus.PENDING; }
    public boolean isPaid()       { return paymentStatus == PaymentStatus.PAID; }

    public OrderSnapshot snapshot() {
        return OrderSnapshot.of(restaurantId,
                items.stream().map(it -> new OrderSnapshot.SnapshotItem(
                        it.getMenuItemId(), it.getItemName(), it.getQuantity(), it.getUnitPrice())).toList(),
                totalAmount);
    }

    // ---- Getters (immutable views) ----------------------------------------

    public OrderId id() { return id; }
    public CustomerId customerId() { return customerId; }
    public RestaurantId restaurantId() { return restaurantId; }
    public List<OrderItem> items() { return Collections.unmodifiableList(items); }
    public Money totalAmount() { return totalAmount; }
    public String customerName() { return customerName; }
    public Address deliveryAddress() { return deliveryAddress; }
    public Email customerEmail() { return customerEmail; }
    public OrderStatus status() { return status; }
    public String rejectReason() { return rejectReason; }
    public String paymentRef() { return paymentRef; }
    public PaymentStatus paymentStatus() { return paymentStatus; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime placedAt() { return placedAt; }
    public LocalDateTime acceptedAt() { return acceptedAt; }
    public LocalDateTime rejectedAt() { return rejectedAt; }
    public LocalDateTime readyAt() { return readyAt; }
    public LocalDateTime pickedUpAt() { return pickedUpAt; }
    public LocalDateTime deliveredAt() { return deliveredAt; }
    public LocalDateTime cancelledAt() { return cancelledAt; }
    public LocalDateTime updatedAt() { return updatedAt; }

    // ---- Helpers ----------------------------------------------------------

    private void requirePending(String op) {
        if (status != OrderStatus.PENDING) {
            throw new OrderFrozenException("Cannot " + op + " — order is " + status);
        }
    }

    private void requireStatus(OrderStatus expected, String op) {
        if (status != expected) {
            throw new OrderFrozenException("Cannot " + op + " — expected " + expected + " but was " + status);
        }
    }

    private void validateConsistency() {
        // CANCELLED is the only non-PENDING state reachable without a placedAt:
        // a PENDING order can be cancelled before it is ever placed.
        if (status != OrderStatus.PENDING && status != OrderStatus.CANCELLED && placedAt == null) {
            throw new IllegalStateException("Non-pending order must have placedAt set");
        }
        if (status == OrderStatus.ACCEPTED && acceptedAt == null) {
            throw new IllegalStateException("Accepted order must have acceptedAt set");
        }
        if (status == OrderStatus.REJECTED && (rejectedAt == null || rejectReason == null)) {
            throw new IllegalStateException("Rejected order must have rejectedAt + rejectReason");
        }
        if (status == OrderStatus.READY_FOR_PICKUP && readyAt == null) {
            throw new IllegalStateException("Ready order must have readyAt set");
        }
        if (status == OrderStatus.PICKED_UP && pickedUpAt == null) {
            throw new IllegalStateException("PickedUp order must have pickedUpAt");
        }
        if (status == OrderStatus.DELIVERED && deliveredAt == null) {
            throw new IllegalStateException("Delivered order must have deliveredAt");
        }
    }

    private void touch() { this.updatedAt = LocalDateTime.now(); }

    private static void requireNonNull(Object value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " must not be null");
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order other)) return false;
        return id.equals(other.id);
    }

    @Override public int hashCode() { return Objects.hash(id); }
}