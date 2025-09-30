package be.kdg.sa.backend.infrastructure;

import be.kdg.sa.backend.domain.Entities.Order;
import be.kdg.sa.backend.domain.Enums.OrderStatus;
import be.kdg.sa.backend.domain.ValueObjects.CustomerId;
import be.kdg.sa.backend.domain.ValueObjects.OrderId;
import be.kdg.sa.backend.domain.ValueObjects.RestaurantId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", schema = "ordering")
@Getter
@Setter
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

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItemJpaEntity> items = new ArrayList<>();

    // Constructors voor test gemak
    public OrderJpaEntity(String id, String customerId, String restaurantId, String deliveryAddress,
                          String customerEmail, OrderStatus status, BigDecimal totalAmount, String currency) {
        this.id = id;
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.deliveryAddress = deliveryAddress;
        this.customerEmail = customerEmail;
        this.status = status;
        this.totalAmount = totalAmount;
        this.currency = currency;
    }

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

        return order;
    }
}