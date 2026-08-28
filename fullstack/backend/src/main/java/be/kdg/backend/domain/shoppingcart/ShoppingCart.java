package be.kdg.backend.domain.shoppingcart;

import be.kdg.backend.domain.ValidationException;
import be.kdg.backend.domain.shared.CustomerId;
import be.kdg.backend.domain.shared.MenuItemId;
import be.kdg.backend.domain.shared.Money;
import be.kdg.backend.domain.shared.Quantity;
import be.kdg.backend.domain.shared.RestaurantId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * ShoppingCart aggregate root.
 *
 * Invariants (US15/US16):
 *  - All items in one cart come from ONE restaurant (pinned on first addItem).
 *  - Same menu item cannot be added with a different price.
 *  - Each {@link CartItem} is owned exclusively by this aggregate.
 */
@AggregateRoot
public class ShoppingCart {

    @Identity
    private final CartId id;
    private final CustomerId customerId;
    private final LocalDateTime createdAt;

    private RestaurantId restaurantId;
    private final List<CartItem> items = new ArrayList<>();
    private LocalDateTime updatedAt;

    public ShoppingCart(CartId id, CustomerId customerId) {
        requireNonNull(id, "id");
        requireNonNull(customerId, "customerId");
        this.id = id;
        this.customerId = customerId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    private ShoppingCart(CartId id, CustomerId customerId, RestaurantId restaurantId,
                        List<CartItem> items, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.items.addAll(items);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ShoppingCart rehydrate(CartId id, CustomerId customerId, RestaurantId restaurantId,
                                         List<CartItem> items, LocalDateTime createdAt, LocalDateTime updatedAt) {
        ShoppingCart cart = new ShoppingCart(id, customerId, restaurantId, items, createdAt, updatedAt);
        cart.validateConsistency();
        return cart;
    }

    // ---- Aggregate behaviour -------------------------------------------------

    public void addItem(MenuItemId menuItemId, String itemName, Quantity quantity,
                        Money unitPrice, RestaurantId itemRestaurantId, int maxItems) {
        requireNonNull(menuItemId, "menuItemId");
        requireNonBlank(itemName, "itemName");
        requireNonNull(quantity, "quantity");
        requireNonNull(unitPrice, "unitPrice");
        requireNonNull(itemRestaurantId, "itemRestaurantId");

        if (!unitPrice.isPositive()) {
            throw new ValidationException("Unit price must be positive");
        }
        enforceSingleRestaurant(itemRestaurantId);

        Optional<CartItem> existing = findItem(menuItemId);
        if (existing.isPresent()) {
            CartItem existingItem = existing.get();
            if (!existingItem.hasSamePrice(unitPrice)) {
                throw new ValidationException(
                        "Cannot add same menu item with different price. Existing=" + existingItem.getUnitPrice()
                                + ", new=" + unitPrice);
            }
            existingItem.increaseQuantity(quantity);
        } else {
            if (items.size() >= maxItems) {
                throw new ValidationException("Cart item cap exceeded (" + maxItems + ")");
            }
            items.add(CartItem.create(menuItemId, itemName, quantity, unitPrice));
        }
        touch();
    }

    public void updateItemQuantity(MenuItemId menuItemId, Quantity newQuantity) {
        CartItem item = requireItem(menuItemId);
        item.updateQuantity(newQuantity);
        touch();
    }

    public void removeItem(MenuItemId menuItemId) {
        requireNonNull(menuItemId, "menuItemId");
        boolean removed = items.removeIf(it -> it.matches(menuItemId));
        if (removed) {
            if (items.isEmpty()) {
                restaurantId = null;
            }
            touch();
        }
    }

    public void clear() {
        items.clear();
        restaurantId = null;
        touch();
    }

    public boolean isEmpty() { return items.isEmpty(); }
    public int itemCount()  { return items.size(); }

    public Money total() {
        return items.stream().map(CartItem::calculateLineTotal).reduce(Money.ZERO, Money::add);
    }

    public List<CartItem> items() { return Collections.unmodifiableList(items); }

    public CartId id()               { return id; }
    public CustomerId customerId()   { return customerId; }
    public RestaurantId restaurantId() { return restaurantId; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime updatedAt() { return updatedAt; }

    // ---- helpers ------------------------------------------------------------

    private Optional<CartItem> findItem(MenuItemId id) {
        return items.stream().filter(it -> it.matches(id)).findFirst();
    }

    private CartItem requireItem(MenuItemId id) {
        return findItem(id).orElseThrow(() -> new ValidationException("Menu item " + id + " is not in cart " + this.id));
    }

    /** US16: a cart must hold items from only one restaurant. */
    private void enforceSingleRestaurant(RestaurantId newRestaurantId) {
        if (restaurantId == null) {
            restaurantId = newRestaurantId;
        } else if (!restaurantId.equals(newRestaurantId)) {
            throw new ValidationException(
                    "Cart already contains items from restaurant " + restaurantId
                            + " — cannot add items from another restaurant " + newRestaurantId);
        }
    }

    private void validateConsistency() {
        if (restaurantId == null && !items.isEmpty()) {
            throw new IllegalStateException("Cart has items but no restaurantId");
        }
    }

    private void touch() { this.updatedAt = LocalDateTime.now(); }

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
        if (!(o instanceof ShoppingCart other)) return false;
        return id.equals(other.id);
    }

    @Override public int hashCode() { return Objects.hash(id); }
}