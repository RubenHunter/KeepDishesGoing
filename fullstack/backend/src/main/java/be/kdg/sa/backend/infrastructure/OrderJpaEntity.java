package be.kdg.sa.backend.infrastructure;

import be.kdg.sa.backend.domain.Order.*;
import be.kdg.sa.backend.domain.Shared.Money;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "orders", schema = "ordering")
@Getter
@NoArgsConstructor
public class OrderJpaEntity {
    @Id
    private String id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "restaurant_id", nullable = false)
    private String restaurantId;

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @CreationTimestamp
    @Column(name = "create_date", nullable = false)
    private LocalDateTime createDate;

    @UpdateTimestamp
    @Column(name = "update_date", nullable = false)
    private LocalDateTime updateDate;

    @Column(name = "order_placed_at")
    private LocalDateTime orderPlacedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItemJpaEntity> items = new ArrayList<>();

    public static OrderJpaEntity fromDomain(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity();
        entity.id = order.getId().getValue();
        entity.customerId = order.getCustomerId().getValue();
        entity.restaurantId = order.getRestaurantId().getValue();
        entity.deliveryAddress = order.getDeliveryAddress();
        entity.customerEmail = order.getCustomerEmail();
        entity.status = order.getStatus();
        entity.totalAmount = order.getTotalAmount().getAmount();
        entity.currency = order.getTotalAmount().getCurrency();
        entity.createDate = order.getCreateDate();
        entity.updateDate = order.getUpdateDate();
        entity.orderPlacedAt = order.getOrderPlacedAt();
        return entity;
    }

    public Order toDomain() {
        List<OrderItem> domainItems = this.items.stream()
                .map(OrderItemJpaEntity::toDomain)
                .collect(Collectors.toList());

        return Order.reconstruct(
                OrderId.of(this.id),
                CustomerId.of(this.customerId),
                RestaurantId.of(this.restaurantId),
                this.deliveryAddress,
                this.customerEmail,
                this.status,
                Money.of(this.totalAmount, this.currency),
                this.createDate,
                this.updateDate,
                this.orderPlacedAt,
                domainItems
        );
    }

    void addOrderItem(OrderItemJpaEntity orderItem) {
        orderItem.setOrder(this);
        this.items.add(orderItem);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderJpaEntity that = (OrderJpaEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}