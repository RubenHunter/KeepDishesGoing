package be.kdg.sa.backend.architecture;

import be.kdg.sa.backend.application.CreateOrderCommand;
import be.kdg.sa.backend.application.OrderApplicationService;
import be.kdg.sa.backend.domain.Order.*;
import be.kdg.sa.backend.domain.Shared.Money;
import be.kdg.sa.backend.infrastructure.JpaOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderApplicationServiceIntegrationTest {

    @Autowired
    private OrderApplicationService orderService;

    @Autowired
    private JpaOrderRepository orderRepository;

    @Test
    void createOrder_shouldPersistOrder() {
        // Given - gebruik .getValue() om String IDs te krijgen
        CreateOrderCommand command = new CreateOrderCommand(
                CustomerId.generate().getValue(),    // .getValue() to get String
                RestaurantId.generate().getValue(),  // .getValue() to get String
                "Teststraat 123, 1000 Brussel",
                "test@example.com",
                List.of(
                        new CreateOrderCommand.OrderItemCommand(
                                "MENU-123",
                                "Pizza Margherita",
                                2,
                                12.50
                        )
                )
        );

        // When
        OrderId orderId = orderService.createOrder(command);

        // Then
        assertThat(orderId).isNotNull();

        Order savedOrder = orderService.getOrder(orderId);
        assertThat(savedOrder).isNotNull();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(savedOrder.getItems()).hasSize(1);
        assertThat(savedOrder.getTotalAmount()).isEqualTo(Money.ofEuros(25.00));
    }

    @Test
    void acceptOrder_shouldChangeStatus() {
        // Given
        OrderId orderId = createTestOrder();

        // When
        orderService.acceptOrder(orderId);

        // Then
        Order order = orderService.getOrder(orderId);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
    }

    @Test
    void rejectOrder_shouldChangeStatus() {
        // Given
        OrderId orderId = createTestOrder();

        // When
        orderService.rejectOrder(orderId, "Out of stock");

        // Then
        Order order = orderService.getOrder(orderId);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
    }

    private OrderId createTestOrder() {
        CreateOrderCommand command = new CreateOrderCommand(
                CustomerId.generate().getValue(),    // .getValue() to get String
                RestaurantId.generate().getValue(),  // .getValue() to get String
                "Teststraat 123, 1000 Brussel",
                "test@example.com",
                List.of(
                        new CreateOrderCommand.OrderItemCommand(
                                "MENU-123",
                                "Pizza Margherita",
                                1,
                                15.00
                        )
                )
        );
        return orderService.createOrder(command);
    }
}