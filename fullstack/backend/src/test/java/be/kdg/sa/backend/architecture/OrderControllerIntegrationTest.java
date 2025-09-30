package be.kdg.sa.backend.architecture;

import be.kdg.sa.backend.api.OrderController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrderControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createOrder_shouldReturnCreated() {
        // Given
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

        // When
        ResponseEntity<OrderController.OrderResponse> response = restTemplate.postForEntity(
                "/api/orders",
                request,
                OrderController.OrderResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().orderId()).isNotNull();
    }

    @Test
    void getOrder_shouldReturnOrder() {
        // Given - eerst een order aanmaken
        OrderController.CreateOrderRequest createRequest = new OrderController.CreateOrderRequest(
                "CUST-123",
                "REST-456",
                "Teststraat 123, 1000 Brussel",
                "test@example.com",
                List.of(
                        new OrderController.OrderItemRequest(
                                "MENU-789",
                                "Pizza Margherita",
                                1,
                                15.00
                        )
                )
        );

        ResponseEntity<OrderController.OrderResponse> createResponse = restTemplate.postForEntity(
                "/api/orders",
                createRequest,
                OrderController.OrderResponse.class
        );

        String orderId = createResponse.getBody().orderId();

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/orders/" + orderId,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(orderId);
    }
}