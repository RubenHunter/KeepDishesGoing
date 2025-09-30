package be.kdg.sa.backend.architecture;

import be.kdg.sa.backend.domain.Entities.MenuItemId;
import be.kdg.sa.backend.domain.Entities.Order;
import be.kdg.sa.backend.domain.Enums.OrderStatus;
import be.kdg.sa.backend.domain.ValueObjects.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {
//g
    @Test
    @DisplayName("Should create order with valid data")
    void createOrder_withValidData_shouldSucceed() {
        // Given
        OrderId orderId = OrderId.generate();
        CustomerId customerId = CustomerId.generate();
        RestaurantId restaurantId = RestaurantId.generate();

        // When
        Order order = new Order(
                orderId,
                customerId,
                restaurantId,
                "Teststraat 123, 1000 Brussel",
                "test@example.com"
        );

        // Then
        assertThat(order.getId()).isEqualTo(orderId);
        assertThat(order.getCustomerId()).isEqualTo(customerId);
        assertThat(order.getRestaurantId()).isEqualTo(restaurantId);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getItems()).isEmpty();
        assertThat(order.getTotalAmount()).isEqualTo(Money.ZERO);
    }
//g
    @Test
    @DisplayName("Should throw exception when creating order with null values")
    void createOrder_withNullValues_shouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> new Order(
                null,
                CustomerId.generate(),
                RestaurantId.generate(),
                "Teststraat 123",
                "test@example.com"
        )).isInstanceOf(IllegalArgumentException.class);
    }

//g
    @Test
    @DisplayName("Should add item to order")
    void addItem_withValidItem_shouldAddToOrder() {
        // Given
        Order order = createPendingOrder();
        MenuItemId menuItemId = MenuItemId.generate();

        // When
        order.addItem(
                menuItemId,
                "Pizza Margherita",
                Quantity.of(2),
                Money.ofEuros(12.50)
        );

        // Then
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getTotalAmount()).isEqualTo(Money.ofEuros(25.00));
    }

    //g
    @Test
    @DisplayName("Should not add item to placed order")
    void addItem_toPlacedOrder_shouldThrowException() {
        // Given
        Order order = createPlacedOrder();

        // When & Then
        assertThatThrownBy(() -> order.addItem(
                MenuItemId.generate(),
                "Pizza",
                Quantity.of(1),
                Money.ofEuros(10.00)
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }
//g
    @Test
    @DisplayName("Should place order with items")
    void placeOrder_withItems_shouldChangeStatus() {
        // Given
        Order order = createOrderWithItems();

        // When
        order.placeOrder();

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
    }
//g
    @Test
    @DisplayName("Should not place empty order")
    void placeOrder_withoutItems_shouldThrowException() {
        // Given
        Order order = createPendingOrder();

        // When & Then
        assertThatThrownBy(order::placeOrder)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least one item");
    }
//g
    @Test
    @DisplayName("Should accept placed order")
    void acceptOrder_whenPlaced_shouldChangeStatus() {
        // Given
        Order order = createPlacedOrder();

        // When
        order.acceptOrder();

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
    }
//g
    @Test
    @DisplayName("Should not accept pending order")
    void acceptOrder_whenPending_shouldThrowException() {
        // Given
        Order order = createPendingOrder();

        // When & Then
        assertThatThrownBy(order::acceptOrder)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PLACED");
    }
//g
    @Test
    @DisplayName("Should reject order with reason")
    void rejectOrder_withReason_shouldChangeStatus() {
        // Given
        Order order = createPlacedOrder();

        // When
        order.rejectOrder("Out of stock");

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
    }
//g
    @Test
    @DisplayName("Should calculate total correctly")
    void calculateTotal_withMultipleItems_shouldReturnCorrectTotal() {
        // Given
        Order order = createPendingOrder();

        // When
        order.addItem(MenuItemId.generate(), "Pizza", Quantity.of(2), Money.ofEuros(10.00));
        order.addItem(MenuItemId.generate(), "Drink", Quantity.of(1), Money.ofEuros(3.50));

        // Then
        assertThat(order.getTotalAmount()).isEqualTo(Money.ofEuros(23.50));
    }

    private Order createPendingOrder() {
        return new Order(
                OrderId.generate(),
                CustomerId.generate(),
                RestaurantId.generate(),
                "Teststraat 123, 1000 Brussel",
                "test@example.com"
        );
    }

    private Order createOrderWithItems() {
        Order order = createPendingOrder();
        order.addItem(MenuItemId.generate(), "Pizza", Quantity.of(1), Money.ofEuros(12.00));
        return order;
    }

    private Order createPlacedOrder() {
        Order order = createOrderWithItems();
        order.placeOrder();
        return order;
    }
}
