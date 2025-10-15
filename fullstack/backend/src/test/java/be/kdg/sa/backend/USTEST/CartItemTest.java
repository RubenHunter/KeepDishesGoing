package be.kdg.sa.backend.USTEST;


import be.kdg.sa.backend.domain.Order.MenuItemId;
import be.kdg.sa.backend.domain.Shared.CartItem;
import be.kdg.sa.backend.domain.Shared.Money;
import be.kdg.sa.backend.domain.Shared.Quantity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CartItem - US16: Mandje items")
class CartItemTest {

    private MenuItemId menuItemId;
    private String itemName;
    private Quantity quantity;
    private Money unitPrice;

    @BeforeEach
    void setUp() {
        menuItemId = MenuItemId.of("MENU-PIZZA");
        itemName = "Margherita Pizza";
        quantity = Quantity.of(2);
        unitPrice = Money.ofEuros(12.50);
    }

    @Test
    @DisplayName("Should create cart item with valid data")
    void shouldCreateCartItemWithValidData() {
        // When
        CartItem cartItem = CartItem.create(menuItemId, itemName, quantity, unitPrice);

        // Then
        assertEquals(menuItemId, cartItem.getMenuItemId());
        assertEquals(itemName, cartItem.getItemName());
        assertEquals(quantity, cartItem.getQuantity());
        assertEquals(unitPrice, cartItem.getUnitPrice());
    }

    @Test
    @DisplayName("Should calculate line total correctly")
    void shouldCalculateLineTotalCorrectly() {
        // Given
        CartItem cartItem = CartItem.create(menuItemId, itemName, quantity, unitPrice);

        // When
        Money lineTotal = cartItem.calculateLineTotal();

        // Then
        assertEquals(Money.ofEuros(25.00), lineTotal); // 2 * 12.50
    }

    @Test
    @DisplayName("Should update quantity correctly")
    void shouldUpdateQuantityCorrectly() {
        // Given
        CartItem cartItem = CartItem.create(menuItemId, itemName, quantity, unitPrice);

        // When
        cartItem.updateQuantity(Quantity.of(5));

        // Then
        assertEquals(5, cartItem.getQuantity().getValue());
        assertEquals(Money.ofEuros(62.50), cartItem.calculateLineTotal()); // 5 * 12.50
    }

    @Test
    @DisplayName("Should increase quantity correctly")
    void shouldIncreaseQuantityCorrectly() {
        // Given
        CartItem cartItem = CartItem.create(menuItemId, itemName, quantity, unitPrice);

        // When
        cartItem.increaseQuantity(Quantity.of(3));

        // Then
        assertEquals(5, cartItem.getQuantity().getValue()); // 2 + 3
        assertEquals(Money.ofEuros(62.50), cartItem.calculateLineTotal()); // 5 * 12.50
    }

    @Test
    @DisplayName("Should decrease quantity correctly")
    void shouldDecreaseQuantityCorrectly() {
        // Given
        CartItem cartItem = CartItem.create(menuItemId, itemName, quantity, unitPrice);

        // When
        cartItem.decreaseQuantity(Quantity.of(1));

        // Then
        assertEquals(1, cartItem.getQuantity().getValue());
        assertEquals(Money.ofEuros(12.50), cartItem.calculateLineTotal());
    }

    @Test
    @DisplayName("Should prevent decreasing quantity below 1")
    void shouldPreventDecreasingQuantityBelow1() {
        // Given
        CartItem cartItem = CartItem.create(menuItemId, itemName, Quantity.of(1), unitPrice);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            cartItem.decreaseQuantity(Quantity.of(1));
        });
    }

    @Test
    @DisplayName("Should identify same menu items correctly")
    void shouldIdentifySameMenuItemsCorrectly() {
        // Given
        CartItem cartItem = CartItem.create(menuItemId, itemName, quantity, unitPrice);
        MenuItemId sameMenuItemId = MenuItemId.of("MENU-PIZZA");
        MenuItemId differentMenuItemId = MenuItemId.of("MENU-PASTA");

        // Then
        assertTrue(cartItem.isSameMenuItem(sameMenuItemId));
        assertFalse(cartItem.isSameMenuItem(differentMenuItemId));
    }

    @Test
    @DisplayName("Should identify same prices correctly")
    void shouldIdentifySamePricesCorrectly() {
        // Given
        CartItem cartItem = CartItem.create(menuItemId, itemName, quantity, unitPrice);
        Money samePrice = Money.ofEuros(12.50);
        Money differentPrice = Money.ofEuros(15.00);

        // Then
        assertTrue(cartItem.hasSamePrice(samePrice));
        assertFalse(cartItem.hasSamePrice(differentPrice));
    }

    @Test
    @DisplayName("Should validate input parameters")
    void shouldValidateInputParameters() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            CartItem.create(null, itemName, quantity, unitPrice);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            CartItem.create(menuItemId, "", quantity, unitPrice);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            CartItem.create(menuItemId, itemName, null, unitPrice);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            CartItem.create(menuItemId, itemName, quantity, null);
        });
    }
}
