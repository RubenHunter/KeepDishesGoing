package be.kdg.backend.infrastructure.persistence.order;

import be.kdg.backend.domain.order.OrderItem;
import be.kdg.backend.domain.order.OrderStatus;
import be.kdg.backend.domain.order.PaymentStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity for {@link be.kdg.backend.domain.order.Order} aggregate.
 * Stored in {@code ordering.orders} table (Hibernate default_schema does the rest).
 */
@Entity
@Table(name = "orders", schema = "ordering")
public class JpaOrderEntity {

    @Id
    private UUID id;

    private UUID customerId;
    private UUID restaurantId;

    private String customerName;

    private String deliveryStreet;
    private String deliveryNumber;
    private String deliveryPostalCode;
    private String deliveryCity;
    private String deliveryCountry;

    private String customerEmail;

    private BigDecimal totalAmount;
    private String currency;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private String rejectReason;
    private String paymentRef;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private LocalDateTime createdAt;
    private LocalDateTime placedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime readyAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    private List<JpaOrderItemEntity> items = new ArrayList<>();

    public JpaOrderEntity() {}

    public static JpaOrderEntity fromDomain(be.kdg.backend.domain.order.Order o) {
        JpaOrderEntity e = new JpaOrderEntity();
        e.id = o.id().value();
        e.customerId = o.customerId().value();
        e.restaurantId = o.restaurantId().value();
        e.customerName = o.customerName();
        e.deliveryStreet = o.deliveryAddress().street();
        e.deliveryNumber = o.deliveryAddress().number();
        e.deliveryPostalCode = o.deliveryAddress().postalCode();
        e.deliveryCity = o.deliveryAddress().city();
        e.deliveryCountry = o.deliveryAddress().country();
        e.customerEmail = o.customerEmail().value();
        e.totalAmount = o.totalAmount().amount();
        e.currency = o.totalAmount().currency();
        e.status = o.status();
        e.rejectReason = o.rejectReason();
        e.paymentRef = o.paymentRef();
        e.paymentStatus = o.paymentStatus();
        e.createdAt = o.createdAt();
        e.placedAt = o.placedAt();
        e.acceptedAt = o.acceptedAt();
        e.rejectedAt = o.rejectedAt();
        e.readyAt = o.readyAt();
        e.pickedUpAt = o.pickedUpAt();
        e.deliveredAt = o.deliveredAt();
        e.cancelledAt = o.cancelledAt();
        e.updatedAt = o.updatedAt();
        o.items().forEach(it -> e.items.add(JpaOrderItemEntity.from(it, e)));
        return e;
    }

    public be.kdg.backend.domain.order.Order toDomain() {
        return be.kdg.backend.domain.order.Order.rehydrate(
                be.kdg.backend.domain.order.OrderId.of(id.toString()),
                be.kdg.backend.domain.shared.CustomerId.of(customerId),
                be.kdg.backend.domain.shared.RestaurantId.of(restaurantId),
                items.stream().map(JpaOrderItemEntity::toDomain).toList(),
                new be.kdg.backend.domain.shared.Money(totalAmount, currency),
                customerName,
                new be.kdg.backend.domain.shared.Address(deliveryStreet, deliveryNumber, deliveryPostalCode, deliveryCity, deliveryCountry),
                new be.kdg.backend.domain.shared.Email(customerEmail),
                status,
                rejectReason,
                paymentRef,
                paymentStatus,
                createdAt, placedAt, acceptedAt, rejectedAt, readyAt, pickedUpAt, deliveredAt, cancelledAt, updatedAt
        );
    }

    // Getters below are minimal — used by Spring Data JPA + tests
    public UUID getId() { return id; }
    public java.util.List<JpaOrderItemEntity> getItems() { return items; }
    public String getPaymentRef() { return paymentRef; }
    public OrderStatus getStatus() { return status; }
}