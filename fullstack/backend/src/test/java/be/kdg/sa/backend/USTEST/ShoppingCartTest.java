package be.kdg.sa.backend.USTEST;

import be.kdg.sa.backend.domain.Order.CustomerId;
import be.kdg.sa.backend.domain.Order.MenuItemId;
import be.kdg.sa.backend.domain.Order.RestaurantId;
import be.kdg.sa.backend.domain.Shared.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ShoppingCart - US16: Mandje met één restaurant")
class ShoppingCartTest {

    private ShoppingCart cart;
    private CustomerId customerId;
    private RestaurantId restaurantPizza;
    private RestaurantId restaurantSushi;
    private MenuItemId pizzaItem;
    private MenuItemId pastaItem;
    private MenuItemId sushiItem;

    @BeforeEach
    void setUp() {
        customerId = CustomerId.generate();
        cart = new ShoppingCart(ShoppingCartId.generate(), customerId);

        restaurantPizza = RestaurantId.of("REST-PIZZA");
        restaurantSushi = RestaurantId.of("REST-SUSHI");

        pizzaItem = MenuItemId.of("MENU-PIZZA");
        pastaItem = MenuItemId.of("MENU-PASTA");
        sushiItem = MenuItemId.of("MENU-SUSHI");
    }

    @Test
    @DisplayName("Should allow adding multiple items from same restaurant")
    void shouldAllowAddingItemsFromSameRestaurant() {
        // When
        cart.addItem(pizzaItem, "Margherita Pizza", Quantity.of(1), Money.ofEuros(12.50), restaurantPizza);
        cart.addItem(pastaItem, "Spaghetti Carbonara", Quantity.of(2), Money.ofEuros(8.75), restaurantPizza);

        // Then
        assertEquals(2, cart.getItemCount());
        assertEquals(restaurantPizza, cart.getRestaurantId());
        assertEquals(Money.ofEuros(30.00), cart.getTotalAmount()); // 12.50 + (2 * 8.75) = 30.00
    }

    @Test
    @DisplayName("Should prevent adding items from different restaurants")
    void shouldPreventAddingItemsFromDifferentRestaurants() {
        // Given
        cart.addItem(pizzaItem, "Margherita Pizza", Quantity.of(1), Money.ofEuros(12.50), restaurantPizza);

        // When & Then
        ShoppingCartRestaurantException exception = assertThrows(ShoppingCartRestaurantException.class, () -> {
            cart.addItem(sushiItem, "California Roll", Quantity.of(1), Money.ofEuros(15.00), restaurantSushi);
        });

        assertTrue(exception.getMessage().contains("Cannot add items from different restaurants"));
        assertTrue(exception.getMessage().contains("REST-PIZZA"));
        assertTrue(exception.getMessage().contains("REST-SUSHI"));
        assertEquals(1, cart.getItemCount());
        assertEquals(restaurantPizza, cart.getRestaurantId());
    }

    @Test
    @DisplayName("Should set restaurant ID when adding first item")
    void shouldSetRestaurantIdWhenAddingFirstItem() {
        // When
        cart.addItem(pizzaItem, "Margherita Pizza", Quantity.of(1), Money.ofEuros(12.50), restaurantPizza);

        // Then
        assertEquals(restaurantPizza, cart.getRestaurantId());
        assertTrue(cart.containsItemsFromRestaurant(restaurantPizza));
    }

    @Test
    @DisplayName("Should clear restaurant ID when cart is emptied")
    void shouldClearRestaurantIdWhenCartIsEmptied() {
        // Given
        cart.addItem(pizzaItem, "Margherita Pizza", Quantity.of(1), Money.ofEuros(12.50), restaurantPizza);
        assertEquals(restaurantPizza, cart.getRestaurantId());

        // When
        cart.clearCart();

        // Then
        assertNull(cart.getRestaurantId());
        assertTrue(cart.isEmpty());
        assertEquals(Money.ZERO, cart.getTotalAmount());
    }

    @Test
    @DisplayName("Should clear restaurant ID when last item is removed")
    void shouldClearRestaurantIdWhenLastItemIsRemoved() {
        // Given
        cart.addItem(pizzaItem, "Margherita Pizza", Quantity.of(1), Money.ofEuros(12.50), restaurantPizza);
        assertEquals(restaurantPizza, cart.getRestaurantId());

        // When
        cart.removeItem(pizzaItem);

        // Then
        assertNull(cart.getRestaurantId());
        assertTrue(cart.isEmpty());
    }

    @Test
    @DisplayName("Should maintain restaurant consistency after reconstruction")
    void shouldMaintainRestaurantConsistencyAfterReconstruction() {
        // Given
        List<CartItem> items = List.of(
                CartItem.create(pizzaItem, "Margherita Pizza", Quantity.of(1), Money.ofEuros(12.50)),
                CartItem.create(pastaItem, "Spaghetti Carbonara", Quantity.of(2), Money.ofEuros(8.75))
        );

        // When
        ShoppingCart reconstructedCart = ShoppingCart.reconstruct(
                ShoppingCartId.generate(),
                customerId,
                restaurantPizza,
                items,
                Money.ofEuros(30.00),
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now()
        );

        // Then
        assertEquals(restaurantPizza, reconstructedCart.getRestaurantId());
        assertEquals(2, reconstructedCart.getItemCount());
        assertTrue(reconstructedCart.containsItemsFromRestaurant(restaurantPizza));
    }

@Test
@DisplayName("Should validate total amount consistency during reconstruction")
void shouldValidateTotalAmountConsistencyDuringReconstruction() {
    // Given - Inconsistent total amount
    List<CartItem> items = List.of(
            CartItem.create(pizzaItem, "Margherita Pizza", Quantity.of(1), Money.ofEuros(12.50))
    );
    Money incorrectTotal = Money.ofEuros(20.00); // Should be 12.50

    // When & Then
    IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
        ShoppingCart.reconstruct(
                ShoppingCartId.generate(),
                customerId,
                restaurantPizza,
                items,
                incorrectTotal,
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now()
        );
    });

    assertTrue(exception.getMessage().contains("Cart total amount is inconsistent"));
}

    @Test
    @DisplayName("Should prevent adding same menu item with different price")
    void shouldPreventAddingSameMenuItemWithDifferentPrice() {
        // Given
        cart.addItem(pizzaItem, "Margherita Pizza", Quantity.of(1), Money.ofEuros(12.50), restaurantPizza);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            cart.addItem(pizzaItem, "Margherita Pizza", Quantity.of(1), Money.ofEuros(15.00), restaurantPizza);
        });

        assertTrue(exception.getMessage().contains("Cannot add same menu item with different price"));
        assertTrue(exception.getMessage().contains("12.50"));
        assertTrue(exception.getMessage().contains("15.00"));
        assertEquals(1, cart.getItemCount());
        assertEquals(Money.ofEuros(12.50), cart.getTotalAmount());
    }

    @Test
    @DisplayName("Should allow reconstruction of empty cart with restaurant ID")
    void shouldAllowReconstructionOfEmptyCartWithRestaurantId() {
        // Given - Empty cart with restaurant ID (bijvoorbeeld na verwijderen laatste item)
        List<CartItem> emptyItems = List.of();

        // When
        ShoppingCart reconstructedCart = ShoppingCart.reconstruct(
                ShoppingCartId.generate(),
                customerId,
                restaurantPizza,
                emptyItems,
                Money.ZERO,
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now()
        );

        // Then - Should not throw exception
        assertNotNull(reconstructedCart);
        assertEquals(restaurantPizza, reconstructedCart.getRestaurantId());
        assertTrue(reconstructedCart.isEmpty());
    }

    @Test
    @DisplayName("Should allow updating quantities within same restaurant")
    void shouldAllowUpdatingQuantitiesWithinSameRestaurant() {
        // Given
        cart.addItem(pizzaItem, "Margherita Pizza", Quantity.of(1), Money.ofEuros(12.50), restaurantPizza);

        // When
        cart.updateItemQuantity(pizzaItem, Quantity.of(3));

        // Then
        assertEquals(1, cart.getItemCount());
        assertEquals(restaurantPizza, cart.getRestaurantId());
        assertEquals(Money.ofEuros(37.50), cart.getTotalAmount()); // 3 * 12.50
    }

    @Test
    @DisplayName("Should allow adding same menu item multiple times")
    void shouldAllowAddingSameMenuItemMultipleTimes() {
        // When
        cart.addItem(pizzaItem, "Margherita Pizza", Quantity.of(1), Money.ofEuros(12.50), restaurantPizza);
        cart.addItem(pizzaItem, "Margherita Pizza", Quantity.of(2), Money.ofEuros(12.50), restaurantPizza);

        // Then
        assertEquals(1, cart.getItemCount()); // Same item, so count should be 1
        assertEquals(3, cart.getItems().get(0).getQuantity().getValue()); // But quantity should be 3
        assertEquals(restaurantPizza, cart.getRestaurantId());
    }

    @Test
    @DisplayName("Should correctly identify if cart contains items from restaurant")
    void shouldCorrectlyIdentifyIfCartContainsItemsFromRestaurant() {
        // Given
        cart.addItem(pizzaItem, "Margherita Pizza", Quantity.of(1), Money.ofEuros(12.50), restaurantPizza);

        // Then
        assertTrue(cart.containsItemsFromRestaurant(restaurantPizza));
        assertFalse(cart.containsItemsFromRestaurant(restaurantSushi));
        assertFalse(cart.containsItemsFromRestaurant(RestaurantId.of("REST-UNKNOWN")));
    }

    @Test
    @DisplayName("Should handle empty cart restaurant checks")
    void shouldHandleEmptyCartRestaurantChecks() {
        // Then
        assertFalse(cart.containsItemsFromRestaurant(restaurantPizza));
        assertFalse(cart.containsItemsFromRestaurant(restaurantSushi));
        assertNull(cart.getRestaurantId());
    }
}