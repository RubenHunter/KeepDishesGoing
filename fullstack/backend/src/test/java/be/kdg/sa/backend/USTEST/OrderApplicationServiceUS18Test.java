package be.kdg.sa.backend.USTEST;

import be.kdg.sa.backend.application.OrderApplicationService;
import be.kdg.sa.backend.application.OrderNotFoundException;
import be.kdg.sa.backend.domain.Order.*;
import be.kdg.sa.backend.domain.OrderRepository;
import be.kdg.sa.backend.domain.Shared.Money;
import be.kdg.sa.backend.domain.Shared.Quantity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("OrderApplicationService - US18 Integration")
class OrderApplicationServiceUS18Test {

    @Autowired
    private OrderApplicationService orderService;

    @MockBean
    private OrderRepository orderRepository;

    private OrderId orderId;
    private Order order;

    @BeforeEach
    void setUp() {
        orderId = OrderId.generate();
        order = new Order(
                orderId,
                CustomerId.of("CUST-1"),
                RestaurantId.of("REST-1"),
                "123 Main St",
                "customer@example.com"
        );
        order.addItem(MenuItemId.of("MENU-1"), "Pizza", Quantity.of(2), Money.ofEuros(12.50));
    }
//ng
    @Test
    @DisplayName("Should place order and freeze content through service")
    void shouldPlaceOrderAndFreezeContentThroughService() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.placeOrder(orderId);

        verify(orderRepository).save(argThat(savedOrder ->
                savedOrder.getStatus() == OrderStatus.PLACED &&
                        savedOrder.isPlaced() &&
                        savedOrder.getOrderPlacedAt() != null
        ));
    }

    @Test
    @DisplayName("Should return true for modifiable PENDING order")
    void shouldReturnTrueForModifiablePendingOrder() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        boolean canModify = orderService.canModifyOrder(orderId);

        assertTrue(canModify);
    }
//ng
    @Test
    @DisplayName("Should return false for non-modifiable PLACED order")
    void shouldReturnFalseForNonModifiablePlacedOrder() {
        order.placeOrder();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        boolean canModify = orderService.canModifyOrder(orderId);

        assertFalse(canModify);
    }
//ng
    @Test
    @DisplayName("Should return frozen order details")
    void shouldReturnFrozenOrderDetails() {
        order.placeOrder();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderApplicationService.OrderDetails details = orderService.getOrderDetails(orderId);

        assertEquals(orderId, details.orderId());
        assertEquals(OrderStatus.PLACED, details.status());
        assertTrue(details.isPlaced());
        assertNotNull(details.orderPlacedAt());
        assertEquals(order.getTotalAmount(), details.totalAmount());
    }

    @Test
    @DisplayName("Should throw exception when placing non-existent order")
    void shouldThrowExceptionWhenPlacingNonExistentOrder() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        OrderNotFoundException exception = assertThrows(OrderNotFoundException.class, () -> {
            orderService.placeOrder(orderId);
        });

        assertTrue(exception.getMessage().contains("Order not found"));
    }

    @Test
    @DisplayName("Should return false for modifiable status of non-existent order")
    void shouldReturnFalseForModifiableStatusOfNonExistentOrder() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        boolean canModify = orderService.canModifyOrder(orderId);

        assertFalse(canModify);
    }
}