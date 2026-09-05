package be.kdg.backend.domain.shoppingcart;

import be.kdg.backend.domain.shared.MenuItemId;
import be.kdg.backend.domain.shared.Money;
import be.kdg.backend.domain.shared.Quantity;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

import java.util.Objects;

/**
 * Entity inside {@link ShoppingCart} aggregate root. Owns its line-total behaviour.
 * Identity = {@code MenuItemId} (one entry per menu item within a cart).
 */
@Entity
public class CartItem {
    @Identity
    private final MenuItemId menuItemId;
    private final String itemName;
    private final Money unitPrice;
    private Quantity quantity;

    CartItem(MenuItemId menuItemId, String itemName, Quantity quantity, Money unitPrice) {
        requireNonBlank(menuItemId, "menuItemId");
        requireNonBlank(itemName, "itemName");
        requireNonNull(quantity, "quantity");
        requireNonNull(unitPrice, "unitPrice");
        if (!unitPrice.isPositive()) {
            throw new IllegalArgumentException("Cart item unit price must be positive");
        }
        this.menuItemId = menuItemId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public static CartItem create(MenuItemId menuItemId, String itemName, Quantity quantity, Money unitPrice) {
        return new CartItem(menuItemId, itemName, quantity, unitPrice);
    }

    void increaseQuantity(Quantity delta) {
        requireNonNull(delta, "delta");
        this.quantity = this.quantity.plus(delta);
    }

    void updateQuantity(Quantity newQuantity) {
        requireNonNull(newQuantity, "newQuantity");
        this.quantity = newQuantity;
    }

    public Money calculateLineTotal() {
        return unitPrice.multiply(quantity.value());
    }

    public boolean matches(MenuItemId other) { return menuItemId.equals(other); }
    public boolean hasSamePrice(Money otherPrice) { return unitPrice.equals(otherPrice); }

    public MenuItemId getMenuItemId() { return menuItemId; }
    public String getItemName()        { return itemName; }
    public Quantity getQuantity()      { return quantity; }
    public Money getUnitPrice()        { return unitPrice; }

    private static void requireNonBlank(Object value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " must not be null");
        if (value instanceof String s && s.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    private static void requireNonNull(Object value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " must not be null");
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CartItem other)) return false;
        return menuItemId.equals(other.menuItemId);
    }

    @Override public int hashCode() { return Objects.hash(menuItemId); }
}