package be.kdg.sa.backend.domain.Order;

import be.kdg.sa.backend.domain.Shared.Money;
import be.kdg.sa.backend.domain.Shared.Quantity;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

import java.util.Objects;

@Entity
public class OrderItem {
    @Identity
    private final MenuItemId menuItemId;

    private final String itemName;
    private Quantity quantity;
    private final Money unitPrice;

    public OrderItem(MenuItemId menuItemId, String itemName, Quantity quantity, Money unitPrice) {
        if (menuItemId == null) {
            throw new IllegalArgumentException("MenuItemId cannot be null");
        }
        if (itemName == null || itemName.isBlank()) {
            throw new IllegalArgumentException("Item name cannot be null or empty");
        }
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity cannot be null");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("Unit price cannot be null");
        }

        // Gebruik de beschikbare validatie van Quantity
        if (quantity.getValue() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        // Money valideert zichzelf al in de constructor (geen negatieve bedragen)

        this.menuItemId = menuItemId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public void updateQuantity(Quantity newQuantity) {
        if (newQuantity == null) {
            throw new IllegalArgumentException("Quantity cannot be null");
        }
        if (newQuantity.getValue() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.quantity = newQuantity;
    }

    public void increaseQuantity(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        // Gebruik de factory method van Quantity
        this.quantity = Quantity.of(this.quantity.getValue() + amount);
    }

    public void decreaseQuantity(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        int newQuantity = this.quantity.getValue() - amount;
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Quantity would become non-positive");
        }
        this.quantity = Quantity.of(newQuantity);
    }

    public Money calculateLineTotal() {
        return unitPrice.multiply(quantity.getValue());
    }

    // Business method om te checken of dit hetzelfde menu item is
    public boolean isSameMenuItem(MenuItemId otherMenuItemId) {
        return this.menuItemId.equals(otherMenuItemId);
    }

    // Business method om te checken of dit item dezelfde prijs heeft
    public boolean hasSamePrice(Money otherUnitPrice) {
        return this.unitPrice.equals(otherUnitPrice);
    }

    // Getters
    public MenuItemId getMenuItemId() {
        return menuItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    // Equality based on MenuItemId - zelfde menu item =zelfde order item
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return Objects.equals(menuItemId, orderItem.menuItemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(menuItemId);
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "menuItemId=" + menuItemId +
                ", itemName='" + itemName + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", lineTotal=" + calculateLineTotal() +
                '}';
    }
}