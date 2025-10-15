package be.kdg.sa.backend.infrastructure;

import be.kdg.sa.backend.domain.Order.MenuItemId;
import be.kdg.sa.backend.domain.Order.OrderItem;
import be.kdg.sa.backend.domain.Shared.Money;
import be.kdg.sa.backend.domain.Shared.Quantity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items", schema = "ordering")
@Getter
@NoArgsConstructor
public class OrderItemJpaEntity {
    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "menu_item_id", nullable = false)
    private String menuItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @Setter
    private OrderJpaEntity order;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "currency", nullable = false)
    private String currency;

    public static OrderItemJpaEntity fromDomain(OrderItem orderItem, OrderJpaEntity order) {
        OrderItemJpaEntity entity = new OrderItemJpaEntity();
        entity.id = UUID.randomUUID().toString();
        entity.menuItemId = orderItem.getMenuItemId().getValue();
        entity.order = order;
        entity.itemName = orderItem.getItemName();
        entity.quantity = orderItem.getQuantity().getValue();
        entity.unitPrice = orderItem.getUnitPrice().getAmount();
        entity.currency = orderItem.getUnitPrice().getCurrency();
        return entity;
    }

    public OrderItem toDomain() {
        return OrderItem.create(
                MenuItemId.of(this.menuItemId),
                this.itemName,
                Quantity.of(this.quantity),
                Money.of(this.unitPrice, this.currency)
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItemJpaEntity that = (OrderItemJpaEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}