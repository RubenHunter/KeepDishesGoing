package be.kdg.backend.domain.order;

import be.kdg.backend.domain.shared.MenuItemId;
import be.kdg.backend.domain.shared.Money;
import be.kdg.backend.domain.shared.Quantity;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

import java.util.Objects;

/**
 * Order line — entity inside the {@link Order} aggregate root.
 * Identity = {@link MenuItemId}.
 * Name + unit price immutably frozen from the checkout snapshot (US18).
 */
@Entity
public class OrderItem {
    @Identity
    private final MenuItemId menuItemId;
    private final String itemName;
    private final Money unitPrice;
    private final Quantity quantity;

    OrderItem(MenuItemId menuItemId, String itemName, Quantity quantity, Money unitPrice) {
        requireNonNull(menuItemId, "menuItemId");
        requireNonBlank(itemName, "itemName");
        requireNonNull(quantity, "quantity");
        requireNonNull(unitPrice, "unitPrice");
        if (!unitPrice.isPositive()) {
            throw new IllegalArgumentException("Order item unit price must be positive");
        }
        this.menuItemId = menuItemId;
        this.itemName   = itemName;
        this.quantity   = quantity;
        this.unitPrice  = unitPrice;
    }

    public static OrderItem create(MenuItemId menuItemId, String itemName, Quantity quantity, Money unitPrice) {
        return new OrderItem(menuItemId, itemName, quantity, unitPrice);
    }

    public Money calculateLineTotal() {
        return unitPrice.multiply(quantity.value());
    }

    public MenuItemId getMenuItemId() { return menuItemId; }
    public String getItemName()       { return itemName; }
    public Quantity getQuantity()     { return quantity; }
    public Money getUnitPrice()       { return unitPrice; }

    private static void requireNonNull(Object value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " must not be null");
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItem other)) return false;
        return menuItemId.equals(other.menuItemId);
    }

    @Override public int hashCode() { return Objects.hash(menuItemId); }
}