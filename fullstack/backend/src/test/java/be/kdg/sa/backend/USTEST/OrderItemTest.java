package be.kdg.sa.backend.USTEST;

import be.kdg.sa.backend.domain.Order.MenuItemId;
import be.kdg.sa.backend.domain.Order.OrderItem;
import be.kdg.sa.backend.domain.Shared.Money;
import be.kdg.sa.backend.domain.Shared.Quantity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrderItem - US18: Bevroren prijzen in order items")
class OrderItemTest {

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
    @DisplayName("Should create order item with factory method")
    void shouldCreateOrderItemWithFactoryMethod() {
        OrderItem orderItem = OrderItem.create(menuItemId, itemName, quantity, unitPrice);

        assertEquals(menuItemId, orderItem.getMenuItemId());
        assertEquals(itemName, orderItem.getItemName());
        assertEquals(quantity, orderItem.getQuantity());
        assertEquals(unitPrice, orderItem.getUnitPrice());
    }

    @Test
    @DisplayName("Should calculate line total correctly")
    void shouldCalculateLineTotalCorrectly() {
        OrderItem orderItem = OrderItem.create(menuItemId, itemName, quantity, unitPrice);

        Money lineTotal = orderItem.calculateLineTotal();

        assertEquals(Money.ofEuros(25.00), lineTotal);
    }

    @Test
    @DisplayName("Should update quantity correctly")
    void shouldUpdateQuantityCorrectly() {
        OrderItem orderItem = OrderItem.create(menuItemId, itemName, quantity, unitPrice);

        orderItem.updateQuantity(Quantity.of(5));

        assertEquals(5, orderItem.getQuantity().getValue());
        assertEquals(Money.ofEuros(62.50), orderItem.calculateLineTotal());
    }

    @Test
    @DisplayName("Should identify same menu items correctly")
    void shouldIdentifySameMenuItemsCorrectly() {
        OrderItem orderItem = OrderItem.create(menuItemId, itemName, quantity, unitPrice);
        MenuItemId sameMenuItemId = MenuItemId.of("MENU-PIZZA");
        MenuItemId differentMenuItemId = MenuItemId.of("MENU-PASTA");

        assertTrue(orderItem.isSameMenuItem(sameMenuItemId));
        assertFalse(orderItem.isSameMenuItem(differentMenuItemId));
    }

    @Test
    @DisplayName("Should identify same prices correctly")
    void shouldIdentifySamePricesCorrectly() {
        OrderItem orderItem = OrderItem.create(menuItemId, itemName, quantity, unitPrice);
        Money samePrice = Money.ofEuros(12.50);
        Money differentPrice = Money.ofEuros(15.00);

        assertTrue(orderItem.hasSamePrice(samePrice));
        assertFalse(orderItem.hasSamePrice(differentPrice));
    }

    @Test
    @DisplayName("Should validate input parameters")
    void shouldValidateInputParameters() {
        assertThrows(IllegalArgumentException.class, () -> {
            OrderItem.create(null, itemName, quantity, unitPrice);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            OrderItem.create(menuItemId, "", quantity, unitPrice);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            OrderItem.create(menuItemId, itemName, null, unitPrice);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            OrderItem.create(menuItemId, itemName, quantity, null);
        });
    }
}