package be.kdg.backend.domain.shoppingcart;

import be.kdg.backend.domain.ValidationException;
import be.kdg.backend.domain.shared.CustomerId;
import be.kdg.backend.domain.shared.MenuItemId;
import be.kdg.backend.domain.shared.Money;
import be.kdg.backend.domain.shared.Quantity;
import be.kdg.backend.domain.shared.RestaurantId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ShoppingCartTest {

    private final UUID restA = UUID.randomUUID();
    private final UUID restB = UUID.randomUUID();

    private ShoppingCart newCart() {
        return new ShoppingCart(CartId.generate(), CustomerId.generate());
    }

    @Test
    void emptyCartHasNoRestaurant() {
        ShoppingCart cart = newCart();
        assertNull(cart.restaurantId());
        assertTrue(cart.isEmpty());
        assertEquals(0, cart.itemCount());
        assertTrue(cart.total().isZero());
    }

    @Test
    void addItemPinsRestaurant() {
        ShoppingCart cart = newCart();
        cart.addItem(MenuItemId.of(UUID.randomUUID()), "Pizza", Quantity.of(1), Money.ofEuros(10), RestaurantId.of(restA), 50);
        assertEquals(restA, cart.restaurantId().value());
        assertEquals(1, cart.itemCount());
        assertEquals(0, cart.total().amount().compareTo(java.math.BigDecimal.valueOf(10)));
    }

    @Test
    void addingSameItemAddsQuantity() {
        UUID mi = UUID.randomUUID();
        ShoppingCart cart = newCart();
        cart.addItem(MenuItemId.of(mi), "Pizza", Quantity.of(1), Money.ofEuros(10), RestaurantId.of(restA), 50);
        cart.addItem(MenuItemId.of(mi), "Pizza", Quantity.of(2), Money.ofEuros(10), RestaurantId.of(restA), 50);
        assertEquals(1, cart.itemCount());
        assertEquals(3, cart.items().get(0).getQuantity().value());
        assertEquals(0, cart.total().amount().compareTo(java.math.BigDecimal.valueOf(30)));
    }

    @Test
    void addingSameItemDifferentPriceThrows() {
        UUID mi = UUID.randomUUID();
        ShoppingCart cart = newCart();
        cart.addItem(MenuItemId.of(mi), "Pizza", Quantity.of(1), Money.ofEuros(10), RestaurantId.of(restA), 50);
        ValidationException ex = assertThrows(ValidationException.class,
                () -> cart.addItem(MenuItemId.of(mi), "Pizza", Quantity.of(1), Money.ofEuros(11), RestaurantId.of(restA), 50));
        assertTrue(ex.getMessage().contains("different price"));
    }

    @Test
    void addingItemFromDifferentRestaurantThrows() {
        ShoppingCart cart = newCart();
        cart.addItem(MenuItemId.of(UUID.randomUUID()), "Pizza", Quantity.of(1), Money.ofEuros(10), RestaurantId.of(restA), 50);
        ValidationException ex = assertThrows(ValidationException.class,
                () -> cart.addItem(MenuItemId.of(UUID.randomUUID()), "Burger", Quantity.of(1), Money.ofEuros(8), RestaurantId.of(restB), 50));
        assertTrue(ex.getMessage().contains("another restaurant"));
    }

    @Test
    void updateQuantity() {
        UUID mi = UUID.randomUUID();
        ShoppingCart cart = newCart();
        cart.addItem(MenuItemId.of(mi), "Pizza", Quantity.of(1), Money.ofEuros(10), RestaurantId.of(restA), 50);
        cart.updateItemQuantity(MenuItemId.of(mi), Quantity.of(5));
        assertEquals(5, cart.items().get(0).getQuantity().value());
    }

    @Test
    void removeItemClearsRestaurantWhenEmpty() {
        UUID mi = UUID.randomUUID();
        ShoppingCart cart = newCart();
        cart.addItem(MenuItemId.of(mi), "Pizza", Quantity.of(1), Money.ofEuros(10), RestaurantId.of(restA), 50);
        cart.removeItem(MenuItemId.of(mi));
        assertTrue(cart.isEmpty());
        assertNull(cart.restaurantId());
    }

    @Test
    void clearCartResetsState() {
        ShoppingCart cart = newCart();
        cart.addItem(MenuItemId.of(UUID.randomUUID()), "Pizza", Quantity.of(2), Money.ofEuros(10), RestaurantId.of(restA), 50);
        cart.clear();
        assertTrue(cart.isEmpty());
        assertNull(cart.restaurantId());
        assertTrue(cart.total().isZero());
    }

    @Test
    void rejectsZeroUnitPrice() {
        ShoppingCart cart = newCart();
        assertThrows(ValidationException.class,
                () -> cart.addItem(MenuItemId.of(UUID.randomUUID()), "Pizza", Quantity.of(1), Money.ofEuros(0), RestaurantId.of(restA), 50));
    }

    @Test
    void itemsListIsImmutable() {
        ShoppingCart cart = newCart();
        cart.addItem(MenuItemId.of(UUID.randomUUID()), "Pizza", Quantity.of(1), Money.ofEuros(10), RestaurantId.of(restA), 50);
        assertThrows(UnsupportedOperationException.class, () -> cart.items().add(null));
    }
}