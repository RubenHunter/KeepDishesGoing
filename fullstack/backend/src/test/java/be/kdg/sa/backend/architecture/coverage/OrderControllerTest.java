package be.kdg.sa.backend.architecture.coverage;

import be.kdg.sa.backend.api.OrderController;
import be.kdg.sa.backend.application.CreateOrderCommand;
import be.kdg.sa.backend.application.OrderApplicationService;
import be.kdg.sa.backend.application.OrderNotFoundException;
import be.kdg.sa.backend.domain.Entities.MenuItemId;
import be.kdg.sa.backend.domain.Entities.Order;
import be.kdg.sa.backend.domain.ValueObjects.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderApplicationService orderService;
//g
    @Test
    void createOrder_shouldReturnCreated() throws Exception {
        // Arrange
        OrderId orderId = OrderId.of("ORD-123");
        given(orderService.createOrder(any(CreateOrderCommand.class))).willReturn(orderId);

        OrderController.CreateOrderRequest request = new OrderController.CreateOrderRequest(
                "CUST-123",
                "REST-456",
                "Teststraat 123, 1000 Brussel",
                "test@example.com",
                List.of(
                        new OrderController.OrderItemRequest(
                                "MENU-789",
                                "Pizza Margherita",
                                2,
                                12.50
                        )
                )
        );

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", "/api/orders/ORD-123"))
                .andExpect(jsonPath("$.orderId").value("ORD-123"))
                .andExpect(jsonPath("$.message").value("Order created successfully"));
    }
//ng
    @Test
    void createOrder_withInvalidData_shouldReturnBadRequest() throws Exception {
        // Arrange - missing required fields
        OrderController.CreateOrderRequest invalidRequest = new OrderController.CreateOrderRequest(
                "", // empty customerId
                "", // empty restaurantId
                "", // empty address
                "invalid-email", // invalid email
                List.of() // empty items
        );

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
//G
    @Test
    void getOrder_shouldReturnOrder() throws Exception {
        // Arrange
        OrderId orderId = OrderId.of("ORD-123");
        Order order = createTestOrder(orderId);
        given(orderService.getOrder(orderId)).willReturn(order);

        // Act & Assert
        mockMvc.perform(get("/api/orders/ORD-123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id.value").value("ORD-123"))
                .andExpect(jsonPath("$.status").value("PLACED"));
    }
//ng
    @Test
    void getOrder_withNonExistingId_shouldReturnNotFound() throws Exception {
        // Arrange
        OrderId orderId = OrderId.of("NON-EXISTENT");
        given(orderService.getOrder(orderId))
                .willThrow(new OrderNotFoundException("Order not found"));

        // Act & Assert
        mockMvc.perform(get("/api/orders/NON-EXISTENT")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
//g
    @Test
    void acceptOrder_shouldReturnOk() throws Exception {
        // Arrange
        OrderId orderId = OrderId.of("ORD-123");

        // Act & Assert
        mockMvc.perform(post("/api/orders/ORD-123/accept"))
                .andExpect(status().isOk());
    }
//g
    @Test
    void rejectOrder_shouldReturnOk() throws Exception {
        // Arrange
        OrderController.RejectOrderRequest request = new OrderController.RejectOrderRequest("Out of stock");

        // Act & Assert
        mockMvc.perform(post("/api/orders/ORD-123/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
//g
    @Test
    void markAsReadyForPickup_shouldReturnOk() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/orders/ORD-123/ready-for-pickup"))
                .andExpect(status().isOk());
    }
//g
    @Test
    void cancelOrder_shouldReturnOk() throws Exception {
        // Arrange
        OrderController.CancelOrderRequest request = new OrderController.CancelOrderRequest("Customer cancelled");

        // Act & Assert
        mockMvc.perform(post("/api/orders/ORD-123/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
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
}