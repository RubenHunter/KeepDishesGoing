package be.kdg.sa.backend.domain.Entities;

import be.kdg.sa.backend.domain.ValueObjects.Money;
import be.kdg.sa.backend.domain.ValueObjects.OrderItemId;
import be.kdg.sa.backend.domain.ValueObjects.Quantity;
import lombok.Getter;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

import java.util.Objects;

@Entity
public class OrderItem {
    @Identity
    @Getter
    private final OrderItemId id;

    @Getter
    private final MenuItemId menuItemId;

    @Getter
    private final String itemName;

    @Getter
    private Quantity quantity;

    @Getter
    private final Money unitPrice;

    public OrderItem(OrderItemId id, MenuItemId menuItemId, String itemName, Quantity quantity, Money unitPrice) {
        if (id == null || menuItemId == null || itemName == null || quantity == null || unitPrice == null) {
            throw new IllegalArgumentException("OrderItem fields cannot be null");
        }
        if (itemName.isBlank()) {
            throw new IllegalArgumentException("Item name cannot be blank");
        }

        this.id = id;
        this.menuItemId = menuItemId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public void updateQuantity(Quantity newQuantity) {
        if (newQuantity == null) {
            throw new IllegalArgumentException("Quantity cannot be null");
        }
        this.quantity = newQuantity;
    }

    public Money calculateLineTotal() {
        return unitPrice.multiply(quantity.getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return Objects.equals(id, orderItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}