package be.kdg.sa.backend.domain.Shared;

import be.kdg.sa.backend.domain.Order.CustomerId;
import be.kdg.sa.backend.domain.Order.MenuItemId;
import be.kdg.sa.backend.domain.Order.RestaurantId;
import lombok.Getter;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@AggregateRoot
public class ShoppingCart {
    @Identity
    @Getter
    private final ShoppingCartId id;

    @Getter
    private final CustomerId customerId;

    @Getter
    private RestaurantId restaurantId;

    private final List<CartItem> items;

    @Getter
    private Money totalAmount;

    @Getter
    private final LocalDateTime createDate;

    @Getter
    private LocalDateTime updateDate;

    // ✅ Public constructor voor nieuwe carts
    public ShoppingCart(ShoppingCartId id, CustomerId customerId) {
        if (id == null || customerId == null) {
            throw new IllegalArgumentException("ShoppingCart ID and customer ID cannot be null");
        }

        this.id = id;
        this.customerId = customerId;
        this.items = new ArrayList<>();
        this.totalAmount = Money.ZERO;
        this.createDate = LocalDateTime.now();
        this.updateDate = LocalDateTime.now();
    }

    // ✅ Private constructor voor reconstruction
    private ShoppingCart(ShoppingCartId id, CustomerId customerId, RestaurantId restaurantId,
                         List<CartItem> items, Money totalAmount, LocalDateTime createDate, LocalDateTime updateDate) {
        this.id = id;
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.items = new ArrayList<>(items);
        this.totalAmount = totalAmount;
        this.createDate = createDate;
        this.updateDate = updateDate;
    }

    // Factory method voor reconstruction (geen setters!)
    // CORRECTIE: Zorg dat validateCartConsistency wordt aangeroepen
    public static ShoppingCart reconstruct(ShoppingCartId id, CustomerId customerId, RestaurantId restaurantId,
                                           List<CartItem> items, Money totalAmount, LocalDateTime createDate, LocalDateTime updateDate) {
        ShoppingCart cart = new ShoppingCart(id, customerId, restaurantId, items, totalAmount, createDate, updateDate);
        cart.validateCartConsistency(); // ✅ Dit was missing!
        return cart;
    }


    // Rich domain methods (geen getters voor interne state!)
    public void addItem(MenuItemId menuItemId, String itemName, Quantity quantity, Money unitPrice, RestaurantId itemRestaurantId) {
        validateRestaurantConsistency(itemRestaurantId);

        CartItem existingItem = findCartItem(menuItemId);
        if (existingItem != null) {
            // CORRECTIE: Gooi exception bij verschillende prijs
            if (!existingItem.hasSamePrice(unitPrice)) {
                throw new IllegalArgumentException(
                        "Cannot add same menu item with different price. " +
                                "Current price: " + existingItem.getUnitPrice().getAmount() + " " + existingItem.getUnitPrice().getCurrency() +
                                ", New price: " + unitPrice.getAmount() + " " + unitPrice.getCurrency()
                );
            }
            existingItem.increaseQuantity(quantity);
        } else {
            CartItem newItem = CartItem.create(menuItemId, itemName, quantity, unitPrice);
            items.add(newItem);
        }

        recalculateTotal();
        updateTimestamp();
    }

    // ✅ Helper method voor betere price comparison
    public void addItemWithPriceValidation(MenuItemId menuItemId, String itemName, Quantity quantity, Money unitPrice, RestaurantId itemRestaurantId) {
        validateRestaurantConsistency(itemRestaurantId);

        CartItem existingItem = findCartItem(menuItemId);
        if (existingItem != null) {
            // ✅ Strikte price comparison
            if (!existingItem.getUnitPrice().equals(unitPrice)) {
                throw new IllegalArgumentException(
                        String.format("Price mismatch for item %s. Existing: %.2f %s, New: %.2f %s",
                                menuItemId.getValue(),
                                existingItem.getUnitPrice().getAmount().doubleValue(),
                                existingItem.getUnitPrice().getCurrency(),
                                unitPrice.getAmount().doubleValue(),
                                unitPrice.getCurrency())
                );
            }
            existingItem.increaseQuantity(quantity);
        } else {
            CartItem newItem = CartItem.create(menuItemId, itemName, quantity, unitPrice);
            items.add(newItem);
        }

        recalculateTotal();
        updateTimestamp();
    }

    public void removeItem(MenuItemId menuItemId) {
        boolean removed = items.removeIf(item -> item.getMenuItemId().equals(menuItemId));
        if (removed) {
            if (items.isEmpty()) {
                this.restaurantId = null;
            }
            recalculateTotal();
            updateTimestamp();
        }
    }

    public void updateItemQuantity(MenuItemId menuItemId, Quantity newQuantity) {
        CartItem item = findCartItem(menuItemId);
        if (item != null) {
            item.updateQuantity(newQuantity);
            recalculateTotal();
            updateTimestamp();
        }
    }

    public void clearCart() {
        items.clear();
        this.restaurantId = null;
        this.totalAmount = Money.ZERO;
        updateTimestamp();
    }

    // ✅ Business queries (geen directe field access!)
    public boolean isEmpty() {
        return items.isEmpty();
    }

    public boolean containsItemsFromRestaurant(RestaurantId restaurantId) {
        return this.restaurantId != null && this.restaurantId.equals(restaurantId);
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public int getItemCount() {
        return items.size();
    }

    public boolean containsItem(MenuItemId menuItemId) {
        return findCartItem(menuItemId) != null;
    }

    // ✅ Private helper methods
    private CartItem findCartItem(MenuItemId menuItemId) {
        return items.stream()
                .filter(item -> item.getMenuItemId().equals(menuItemId))
                .findFirst()
                .orElse(null);
    }

    private void validateRestaurantConsistency(RestaurantId newRestaurantId) {
        if (this.restaurantId == null) {
            this.restaurantId = newRestaurantId;
        } else if (!this.restaurantId.equals(newRestaurantId)) {
            throw new ShoppingCartRestaurantException(
                    "Cannot add items from different restaurants. Current: " +
                            this.restaurantId.getValue() + ", New: " + newRestaurantId.getValue()
            );
        }
    }

    private void validateCartConsistency() {
        // ✅ Valideer total amount consistency
        Money calculatedTotal = calculateTotalFromItems();

        if (!calculatedTotal.equals(totalAmount)) {
            throw new IllegalStateException(
                    String.format("Cart total amount is inconsistent with items. Calculated: %.2f %s, Stored: %.2f %s",
                            calculatedTotal.getAmount().doubleValue(), calculatedTotal.getCurrency(),
                            totalAmount.getAmount().doubleValue(), totalAmount.getCurrency())
            );
        }

        // ✅ Valideer dat items niet null zijn
        if (items.stream().anyMatch(item -> item == null)) {
            throw new IllegalStateException("Cart contains null items");
        }
    }

    private Money calculateTotalFromItems() {
        return items.stream()
                .map(CartItem::calculateLineTotal)
                .reduce(Money.ZERO, Money::add);
    }

    private void recalculateTotal() {
        this.totalAmount = items.stream()
                .map(CartItem::calculateLineTotal)
                .reduce(Money.ZERO, Money::add);
    }

    private void updateTimestamp() {
        this.updateDate = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShoppingCart that = (ShoppingCart) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}