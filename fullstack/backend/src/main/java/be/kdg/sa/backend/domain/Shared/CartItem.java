package be.kdg.sa.backend.domain.Shared;

import be.kdg.sa.backend.domain.Order.MenuItemId;
import lombok.Getter;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

import java.util.Objects;

@Entity
public class CartItem {
    @Identity
    @Getter
    private final MenuItemId menuItemId;

    @Getter
    private final String itemName;

    private Quantity quantity;

    @Getter
    private final Money unitPrice;

    // ✅ Private constructor
    public CartItem(MenuItemId menuItemId, String itemName, Quantity quantity, Money unitPrice) {
        validateInput(menuItemId, itemName, quantity, unitPrice);
        this.menuItemId = menuItemId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    // ✅ Factory method
    public static CartItem create(MenuItemId menuItemId, String itemName, Quantity quantity, Money unitPrice) {
        return new CartItem(menuItemId, itemName, quantity, unitPrice);
    }

    // ✅ Rich domain behavior
    public void updateQuantity(Quantity newQuantity) {
        if (newQuantity == null) {
            throw new IllegalArgumentException("Quantity cannot be null");
        }
        this.quantity = newQuantity;
    }

    public void increaseQuantity(Quantity amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        this.quantity = Quantity.of(this.quantity.getValue() + amount.getValue());
    }

    public void decreaseQuantity(Quantity amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        int newQuantity = this.quantity.getValue() - amount.getValue();
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Quantity would become non-positive");
        }
        this.quantity = Quantity.of(newQuantity);
    }

    public Money calculateLineTotal() {
        return unitPrice.multiply(quantity.getValue());
    }

    public boolean isSameMenuItem(MenuItemId otherMenuItemId) {
        return this.menuItemId.equals(otherMenuItemId);
    }

    public boolean hasSamePrice(Money otherUnitPrice) {
        if (otherUnitPrice == null) return false;
        // ✅ Strikte equality check voor Money
        return this.unitPrice.getAmount().compareTo(otherUnitPrice.getAmount()) == 0 &&
                this.unitPrice.getCurrency().equals(otherUnitPrice.getCurrency());
    }

    public Quantity getQuantity() {
        return this.quantity;
    }

    private void validateInput(MenuItemId menuItemId, String itemName, Quantity quantity, Money unitPrice) {
        if (menuItemId == null) throw new IllegalArgumentException("MenuItemId cannot be null");
        if (itemName == null || itemName.isBlank()) throw new IllegalArgumentException("Item name cannot be null or empty");
        if (quantity == null) throw new IllegalArgumentException("Quantity cannot be null");
        if (unitPrice == null) throw new IllegalArgumentException("Unit price cannot be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CartItem cartItem = (CartItem) o;
        return Objects.equals(menuItemId, cartItem.menuItemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(menuItemId);
    }
}