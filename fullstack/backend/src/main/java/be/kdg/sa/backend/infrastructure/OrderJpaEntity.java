package be.kdg.sa.backend.infrastructure;

import be.kdg.sa.backend.domain.Entities.Order;
import be.kdg.sa.backend.domain.Enums.OrderStatus;
import be.kdg.sa.backend.domain.ValueObjects.CustomerId;
import be.kdg.sa.backend.domain.ValueObjects.OrderId;
import be.kdg.sa.backend.domain.ValueObjects.RestaurantId;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jmolecules.ddd.annotation.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "orders", schema = "ordering")
@Getter
@NoArgsConstructor
@AllArgsConstructor
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

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
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

        entity.items = order.getItems().stream()
                .map(item -> OrderItemJpaEntity.fromDomain(item, entity))
                .collect(Collectors.toList());

        return entity;
    }

    public Order toDomain() {
        Order order = new Order(
                OrderId.of(this.id),
                CustomerId.of(this.customerId),
                RestaurantId.of(this.restaurantId),
                this.deliveryAddress,
                this.customerEmail
        );

        // Use reflection or package-private methods to set the internal state
        // This is a simplified version - in practice you'd need to properly reconstruct the domain object

        return order;
    }
}
