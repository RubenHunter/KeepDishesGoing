package be.kdg.sa.backend.architecture.coverage;

import be.kdg.sa.backend.application.CreateOrderCommand;
import be.kdg.sa.backend.application.OrderApplicationService;
import be.kdg.sa.backend.application.OrderNotFoundException;
import be.kdg.sa.backend.domain.Entities.MenuItemId;
import be.kdg.sa.backend.domain.Entities.Order;
import be.kdg.sa.backend.domain.Enums.OrderStatus;
import be.kdg.sa.backend.domain.OrderRepository;
import be.kdg.sa.backend.domain.ValueObjects.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceUnitTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderApplicationService sut; // System Under Test

    @Test
    void createOrder_shouldSaveOrderSuccessfully() {
        // Arrange
        CreateOrderCommand command = new CreateOrderCommand(
                "CUST-123",
                "REST-456",
                "Teststraat 123, 1000 Brussel",
                "test@example.com",
                List.of(
                        new CreateOrderCommand.OrderItemCommand(
                                "MENU-789",
                                "Pizza Margherita",
                                2,
                                12.50
                        )
                )
        );

        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderId orderId = sut.createOrder(command);

        // Assert
        assertThat(orderId).isNotNull();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(savedOrder.getItems()).hasSize(1);
        assertThat(savedOrder.getTotalAmount()).isEqualTo(Money.ofEuros(25.00));
    }

    @Test
    void getOrder_shouldReturnOrderWhenExists() {
        // Arrange
        OrderId orderId = OrderId.of("ORD-123");
        Order expectedOrder = createTestOrder(orderId);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(expectedOrder));

        // Act
        Order result = sut.getOrder(orderId);

        // Assert
        assertThat(result).isEqualTo(expectedOrder);
        verify(orderRepository).findById(orderId);
    }

    @Test
    void getOrder_shouldThrowExceptionWhenOrderNotFound() {
        // Arrange
        OrderId orderId = OrderId.of("NON-EXISTENT");
        given(orderRepository.findById(orderId)).willReturn(Optional.empty());

        // Act & Assert
        assertThrows(OrderNotFoundException.class,
                () -> sut.getOrder(orderId));
    }

    @Test
    void acceptOrder_shouldChangeStatusToAccepted() {
        // Arrange
        OrderId orderId = OrderId.of("ORD-123");
        Order order = createPlacedOrder(orderId);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        // Act
        sut.acceptOrder(orderId);

        // Assert
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.ACCEPTED);
    }

    @Test
    void rejectOrder_shouldChangeStatusToRejected() {
        // Arrange
        OrderId orderId = OrderId.of("ORD-123");
        Order order = createPlacedOrder(orderId);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        // Act
        sut.rejectOrder(orderId, "Out of stock");

        // Assert
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.REJECTED);
    }

    @Test
    void markOrderAsReadyForPickup_shouldChangeStatus() {
        // Arrange
        OrderId orderId = OrderId.of("ORD-123");
        Order order = createAcceptedOrder(orderId);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        // Act
        sut.markOrderAsReadyForPickup(orderId);

        // Assert
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.READY_FOR_PICKUP);
    }

    private Order createTestOrder(OrderId orderId) {
        Order order = new Order(
                orderId,
                CustomerId.of("CUST-123"),
                RestaurantId.of("REST-456"),
                "Test Address",
                "test@example.com"
        );
        order.addItem(
                MenuItemId.of("MENU-789"),
                "Pizza Margherita",
                Quantity.of(1),
                Money.ofEuros(15.00)
        );
        order.placeOrder();
        return order;
    }

    private Order createPlacedOrder(OrderId orderId) {
        Order order = createTestOrder(orderId);
        order.placeOrder();
        return order;
    }

    private Order createAcceptedOrder(OrderId orderId) {
        Order order = createPlacedOrder(orderId);
        order.acceptOrder();
        return order;
    }
}