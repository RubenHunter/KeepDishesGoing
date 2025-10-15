package be.kdg.sa.backend.infrastructure;

import be.kdg.sa.backend.domain.Order.MenuItemId;
import be.kdg.sa.backend.domain.Shared.CartItem;
import be.kdg.sa.backend.domain.Shared.Money;
import be.kdg.sa.backend.domain.Shared.Quantity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "cart_items", schema = "ordering")
@Getter
@NoArgsConstructor
public class CartItemJpaEntity {
    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "menu_item_id", nullable = false)
    private String menuItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shopping_cart_id", nullable = false)
    @Setter
    private ShoppingCartJpaEntity shoppingCart;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "currency", nullable = false)
    private String currency;

    public static CartItemJpaEntity fromDomain(CartItem cartItem, ShoppingCartJpaEntity shoppingCart) {
        CartItemJpaEntity entity = new CartItemJpaEntity();
        entity.id = UUID.randomUUID().toString();
        entity.menuItemId = cartItem.getMenuItemId().getValue();
        entity.shoppingCart = shoppingCart;
        entity.itemName = cartItem.getItemName();
        entity.quantity = cartItem.getQuantity().getValue();
        entity.unitPrice = cartItem.getUnitPrice().getAmount();
        entity.currency = cartItem.getUnitPrice().getCurrency();
        return entity;
    }

    public CartItem toDomain() {
        return new CartItem(
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
        CartItemJpaEntity that = (CartItemJpaEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}