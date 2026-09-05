package be.kdg.backend.infrastructure.persistence.cart;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "cart_items", schema = "ordering")
public class JpaCartItemEntity {

    @Id
    private UUID id;

    private UUID menuItemId;
    private String itemName;
    private int quantity;
    private BigDecimal unitPrice;
    private String currency;

    public JpaCartItemEntity() {}

    static JpaCartItemEntity from(be.kdg.backend.domain.shoppingcart.CartItem it, JpaCartEntity owner) {
        JpaCartItemEntity e = new JpaCartItemEntity();
        e.id = UUID.randomUUID();
        e.menuItemId = it.getMenuItemId().value();
        e.itemName = it.getItemName();
        e.quantity = it.getQuantity().value();
        e.unitPrice = it.getUnitPrice().amount();
        e.currency = it.getUnitPrice().currency();
        return e;
    }

    be.kdg.backend.domain.shoppingcart.CartItem toDomain() {
        return be.kdg.backend.domain.shoppingcart.CartItem.create(
                be.kdg.backend.domain.shared.MenuItemId.of(menuItemId),
                itemName,
                be.kdg.backend.domain.shared.Quantity.of(quantity),
                new be.kdg.backend.domain.shared.Money(unitPrice, currency)
        );
    }
}