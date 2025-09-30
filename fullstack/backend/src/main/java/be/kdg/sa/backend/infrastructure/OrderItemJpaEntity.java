package be.kdg.sa.backend.infrastructure;

import be.kdg.sa.backend.domain.Entities.MenuItemId;
import be.kdg.sa.backend.domain.Entities.OrderItem;
import be.kdg.sa.backend.domain.ValueObjects.Money;
import be.kdg.sa.backend.domain.ValueObjects.OrderItemId;
import be.kdg.sa.backend.domain.ValueObjects.Quantity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items", schema = "ordering")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemJpaEntity {
    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderJpaEntity order;

    @Column(name = "menu_item_id", nullable = false)
    private String menuItemId;

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
        entity.id = orderItem.getId().getValue();
        entity.order = order;
        entity.menuItemId = orderItem.getMenuItemId().getValue();
        entity.itemName = orderItem.getItemName();
        entity.quantity = orderItem.getQuantity().getValue();
        entity.unitPrice = orderItem.getUnitPrice().getAmount();
        entity.currency = orderItem.getUnitPrice().getCurrency();
        return entity;
    }

    public OrderItem toDomain() {
        return new OrderItem(
                OrderItemId.of(this.id),
                MenuItemId.of(this.menuItemId),
                this.itemName,
                Quantity.of(this.quantity),
                Money.of(this.unitPrice, this.currency)
        );
    }
}