package be.kdg.backend.domain.shared;

import be.kdg.backend.domain.shoppingcart.ShoppingCart;
import be.kdg.backend.domain.order.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Frozen snapshot taken from a {@link ShoppingCart} at checkout.
 * Carries immutable copies of item name + price (US18: contents/price frozen once order placed).
 */
public record OrderSnapshot(
        RestaurantId restaurantId,
        List<OrderSnapshot.SnapshotItem> items,
        Money total
) {
    public record SnapshotItem(
            MenuItemId menuItemId,
            String itemName,
            Quantity quantity,
            Money unitPrice
    ) {}

    public OrderSnapshot {
        items = List.copyOf(items); // defensive immutable copy
    }

    /** Build directly from a cart (US18 freeze). */
    public static OrderSnapshot from(ShoppingCart cart) {
        List<SnapshotItem> snapshotItems = cart.items().stream()
                .map(ci -> new SnapshotItem(
                        ci.getMenuItemId(),
                        ci.getItemName(),
                        ci.getQuantity(),
                        ci.getUnitPrice()))
                .toList();
        return new OrderSnapshot(cart.restaurantId(), snapshotItems, cart.total());
    }

    /** Used at rehydrate time (DB → Aggregate). */
    public static OrderSnapshot of(RestaurantId restaurantId,
                                   List<SnapshotItem> items,
                                   Money total) {
        return new OrderSnapshot(restaurantId, items, total);
    }
}