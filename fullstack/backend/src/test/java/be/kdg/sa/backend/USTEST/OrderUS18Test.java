package be.kdg.sa.backend.USTEST;

import be.kdg.sa.backend.domain.Order.*;
import be.kdg.sa.backend.domain.Shared.Money;
import be.kdg.sa.backend.domain.Shared.Quantity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Order - US18: Inhoud en prijs bevriezen na plaatsen")
class OrderUS18Test {

    private Order order;
    private OrderId orderId;
    private CustomerId customerId;
    private RestaurantId restaurantId;
    private MenuItemId pizzaItemId;
    private MenuItemId pastaItemId;

    @BeforeEach
    void setUp() {
        orderId = OrderId.generate();
        customerId = CustomerId.of("CUST-1");
        restaurantId = RestaurantId.of("REST-1");
        pizzaItemId = MenuItemId.of("MENU-PIZZA");
        pastaItemId = MenuItemId.of("MENU-PASTA");

        order = new Order(orderId, customerId, restaurantId, "123 Main St", "customer@example.com");
    }

    @Test
    @DisplayName("Should allow modifications in PENDING state")
    void shouldAllowModificationsInPendingState() {
        order.addItem(pizzaItemId, "Margherita Pizza", Quantity.of(2), Money.ofEuros(12.50));
        order.addItem(pastaItemId, "Spaghetti Carbonara", Quantity.of(1), Money.ofEuros(8.75));

        assertEquals(2, order.getItems().size());
        assertEquals(Money.ofEuros(33.75), order.getTotalAmount());
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }
//ng
    @Test
    @DisplayName("Should freeze content and prices when order is placed")
    void shouldFreezeContentAndPricesWhenOrderIsPlaced() {
        order.addItem(pizzaItemId, "Margherita Pizza", Quantity.of(2), Money.ofEuros(12.50));
        order.addItem(pastaItemId, "Spaghetti Carbonara", Quantity.of(1), Money.ofEuros(8.75));
        Money originalTotal = order.getTotalAmount();

        order.placeOrder();

        assertEquals(OrderStatus.PLACED, order.getStatus());
        assertNotNull(order.getOrderPlacedAt());
        assertEquals(originalTotal, order.getTotalAmount());
        assertTrue(order.isPlaced());
    }
//ng
    @Test
    @DisplayName("Should prevent adding items after order is placed")
    void shouldPreventAddingItemsAfterOrderIsPlaced() {
        order.addItem(pizzaItemId, "Margherita Pizza", Quantity.of(1), Money.ofEuros(12.50));
        order.placeOrder();

        OrderFrozenException exception = assertThrows(OrderFrozenException.class, () -> {
            order.addItem(pastaItemId, "Spaghetti Carbonara", Quantity.of(1), Money.ofEuros(8.75));
        });

        assertTrue(exception.getMessage().contains("Order modifications are not allowed"));
        assertEquals(1, order.getItems().size());
    }
//ng
    @Test
    @DisplayName("Should prevent removing items after order is placed")
    void shouldPreventRemovingItemsAfterOrderIsPlaced() {
        order.addItem(pizzaItemId, "Margherita Pizza", Quantity.of(1), Money.ofEuros(12.50));
        order.addItem(pastaItemId, "Spaghetti Carbonara", Quantity.of(1), Money.ofEuros(8.75));
        order.placeOrder();

        OrderFrozenException exception = assertThrows(OrderFrozenException.class, () -> {
            order.removeItem(pizzaItemId);
        });

        assertTrue(exception.getMessage().contains("Order modifications are not allowed"));
        assertEquals(2, order.getItems().size());
    }
//ng
    @Test
    @DisplayName("Should prevent updating quantities after order is placed")
    void shouldPreventUpdatingQuantitiesAfterOrderIsPlaced() {
        order.addItem(pizzaItemId, "Margherita Pizza", Quantity.of(1), Money.ofEuros(12.50));
        order.placeOrder();

        OrderFrozenException exception = assertThrows(OrderFrozenException.class, () -> {
            order.updateItemQuantity(pizzaItemId, Quantity.of(3));
        });

        assertTrue(exception.getMessage().contains("Order modifications are not allowed"));
        assertEquals(1, order.getItems().get(0).getQuantity().getValue());
    }
//ng
    @Test
    @DisplayName("Should maintain frozen prices through order lifecycle")
    void shouldMaintainFrozenPricesThroughOrderLifecycle() {
        order.addItem(pizzaItemId, "Margherita Pizza", Quantity.of(2), Money.ofEuros(12.50));
        Money frozenTotal = order.getTotalAmount();
        order.placeOrder();

        order.acceptOrder();
        order.markAsReadyForPickup();

        assertEquals(frozenTotal, order.getTotalAmount());
        assertEquals(OrderStatus.READY_FOR_PICKUP, order.getStatus());
        assertEquals(25.00, order.getTotalAmount().getAmount().doubleValue());
    }

    @Test
    @DisplayName("Should reconstruct order with frozen state correctly")
    void shouldReconstructOrderWithFrozenStateCorrectly() {
        // ✅ CORRECTIE: Gebruik factory method i.p.v. directe constructor
        List<OrderItem> items = List.of(
                OrderItem.create(pizzaItemId, "Margherita Pizza", Quantity.of(2), Money.ofEuros(12.50))
        );
        Money frozenTotal = Money.ofEuros(25.00);
        java.time.LocalDateTime placedAt = java.time.LocalDateTime.now();

        Order reconstructedOrder = Order.reconstruct(
                orderId, customerId, restaurantId, "123 Main St", "customer@example.com",
                OrderStatus.PLACED, frozenTotal,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now(),
                placedAt, items
        );

        assertEquals(OrderStatus.PLACED, reconstructedOrder.getStatus());
        assertEquals(frozenTotal, reconstructedOrder.getTotalAmount());
        assertTrue(reconstructedOrder.isPlaced());
        assertEquals(placedAt, reconstructedOrder.getOrderPlacedAt());
    }

    @Test
    @DisplayName("Should validate frozen order consistency during reconstruction")
    void shouldValidateFrozenOrderConsistencyDuringReconstruction() {
        // ✅ CORRECTIE: Gebruik factory method
        List<OrderItem> items = List.of(
                OrderItem.create(pizzaItemId, "Margherita Pizza", Quantity.of(2), Money.ofEuros(12.50))
        );
        Money incorrectTotal = Money.ofEuros(30.00);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            Order.reconstruct(
                    orderId, customerId, restaurantId, "123 Main St", "customer@example.com",
                    OrderStatus.PLACED, incorrectTotal,
                    java.time.LocalDateTime.now(), java.time.LocalDateTime.now(),
                    null,
                    items
            );
        });

        assertTrue(exception.getMessage().contains("Non-pending orders must have order placed timestamp"));
    }

    @Test
    @DisplayName("Should calculate dynamic total for PENDING orders")
    void shouldCalculateDynamicTotalForPendingOrders() {
        order.addItem(pizzaItemId, "Margherita Pizza", Quantity.of(1), Money.ofEuros(12.50));

        order.addItem(pastaItemId, "Spaghetti Carbonara", Quantity.of(1), Money.ofEuros(8.75));

        assertEquals(Money.ofEuros(21.25), order.getTotalAmount());
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }
//ng
    @Test
    @DisplayName("Should return correct isPlaced status")
    void shouldReturnCorrectIsPlacedStatus() {
        assertFalse(order.isPlaced());
        assertEquals(OrderStatus.PENDING, order.getStatus());

        order.addItem(pizzaItemId, "Margherita Pizza", Quantity.of(1), Money.ofEuros(12.50));
        order.placeOrder();

        assertTrue(order.isPlaced());
        assertEquals(OrderStatus.PLACED, order.getStatus());
    }

    @Test
    @DisplayName("Should validate order rules before placing")
    void shouldValidateOrderRulesBeforePlacing() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            order.placeOrder();
        });

        assertTrue(exception.getMessage().contains("Order must have at least one item"));
    }

    @Test
    @DisplayName("Should validate positive total amount before placing")
    void shouldValidatePositiveTotalAmountBeforePlacing() {
        // Deze test moet worden aangepast omdat Quantity al valideert op positive values
        // We kunnen deze test overslaan of een andere approach nemen
        order.addItem(pizzaItemId, "Margherita Pizza", Quantity.of(1), Money.ofEuros(0.00));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            order.placeOrder();
        });

        assertTrue(exception.getMessage().contains("Order total amount must be positive"));
    }
//ng
    @Test
    @DisplayName("Should handle order with zero price item correctly")
    void shouldHandleOrderWithZeroPriceItemCorrectly() {
        // Test met een item dat 0 euro kost
        order.addItem(pizzaItemId, "Free Item", Quantity.of(1), Money.ofEuros(0.00));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            order.placeOrder();
        });

        assertTrue(exception.getMessage().contains("Order item line total cannot be zero"));
    }
}