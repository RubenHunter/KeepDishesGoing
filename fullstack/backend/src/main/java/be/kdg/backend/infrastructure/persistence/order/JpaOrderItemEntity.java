package be.kdg.backend.infrastructure.persistence.order;

import be.kdg.backend.domain.order.OrderItem;
import be.kdg.backend.domain.shared.MenuItemId;
import be.kdg.backend.domain.shared.Money;
import be.kdg.backend.domain.shared.Quantity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items", schema = "ordering")
public class JpaOrderItemEntity {

    @Id
    private UUID id;

    private UUID menuItemId;
    private String itemName;
    private int quantity;
    private BigDecimal unitPrice;
    private String currency;

    public JpaOrderItemEntity() {}

    static JpaOrderItemEntity from(OrderItem it, JpaOrderEntity owner) {
        JpaOrderItemEntity e = new JpaOrderItemEntity();
        e.id = UUID.randomUUID();
        e.menuItemId = it.getMenuItemId().value();
        e.itemName = it.getItemName();
        e.quantity = it.getQuantity().value();
        e.unitPrice = it.getUnitPrice().amount();
        e.currency = it.getUnitPrice().currency();
        return e;
    }

    OrderItem toDomain() {
        return OrderItem.create(
                MenuItemId.of(menuItemId),
                itemName,
                Quantity.of(quantity),
                new Money(unitPrice, currency)
        );
    }
}