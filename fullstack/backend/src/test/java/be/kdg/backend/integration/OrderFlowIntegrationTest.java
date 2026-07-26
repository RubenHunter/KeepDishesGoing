package be.kdg.backend.integration;

import be.kdg.backend.application.messaging.EventPublisher;
import be.kdg.backend.application.restaurant.RestaurantGateway;
import be.kdg.backend.domain.order.Order;
import be.kdg.backend.domain.order.OrderRepository;
import be.kdg.backend.domain.order.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration test — same {@code @SpringBootTest} pattern as
 * {@code testing-demo/src/test/java/be/kdg/ordering/OrdersApplicationTests.java}, but exercising
 * the full cart → checkout → place flow against H2 (test profile) with external ports mocked
 * (RestaurantGateway + EventPublisher + RabbitTemplate — rubric: integration mocks only external infra).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class OrderFlowIntegrationTest {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    @Autowired TestRestTemplate http;
    @Autowired OrderRepository orderRepository;

    @MockitoBean RestaurantGateway restaurantGateway;
    @MockitoBean EventPublisher eventPublisher;
    @MockitoBean org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    private UUID knownRestaurantId;

    @BeforeEach
    void wireGateway() {
        knownRestaurantId = UUID.randomUUID();
        when(restaurantGateway.validateMenuItems(any())).thenReturn(
                new RestaurantGateway.MenuValidationResult(
                        true, "OK",
                        List.of(new RestaurantGateway.MenuValidationResult.ItemValidation(
                                UUID.randomUUID(), true, 10.0, "ok"))));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postJson(String url, Object body) {
        ResponseEntity<Map<String, Object>> resp = http.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body), MAP_TYPE);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        return resp.getBody();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postJsonAllowingError(String url, Object body) {
        ResponseEntity<Map<String, Object>> resp = http.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body), MAP_TYPE);
        return resp.getBody() == null ? Map.of() : resp.getBody();
    }

    @Test
    void createCartAddItemsAndCheckout() {
        UUID customerId = UUID.randomUUID();

        // 1. Create cart
        Map<String, Object> cart = postJson("/api/carts", Map.of("customerId", customerId));
        UUID cartId = UUID.fromString((String) cart.get("cartId"));

        // 2. Add item
        Map<String, Object> addBody = Map.of(
                "menuItemId", UUID.randomUUID(),
                "itemName", "Pizza",
                "quantity", 2,
                "unitPrice", 10.0,
                "restaurantId", knownRestaurantId);
        Map<String, Object> cartAfterAdd = postJson(
                "/api/carts/" + cartId + "/items", addBody);
        List<Map<String, Object>> items = (List<Map<String, Object>>) cartAfterAdd.get("items");
        assertThat(items).hasSize(1);

        // 3. Checkout
        Map<String, Object> checkoutBody = Map.of(
                "cartId", cartId,
                "customerId", customerId,
                "customerName", "Ruben",
                "street", "Langestraat",
                "number", "1",
                "postalCode", "2000",
                "city", "Antwerpen",
                "country", "BE",
                "email", "ruben@example.com");
        Map<String, Object> checkoutResp = postJson("/api/orders/checkout", checkoutBody);
        UUID orderId = UUID.fromString((String) checkoutResp.get("orderId"));
        String paymentRef = (String) checkoutResp.get("paymentRef");
        assertThat(paymentRef).startsWith("pay_");

        // 4. Confirm payment (stub → PAID)
        ResponseEntity<Void> confirmResp = http.getForEntity(
                "/api/payments/" + paymentRef + "/confirm", Void.class);
        assertThat(confirmResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 5. Place order — should succeed (PAID) and emit event
        ResponseEntity<Void> placeResp = http.postForEntity(
                "/api/orders/" + orderId + "/place", null, Void.class);
        assertThat(placeResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(eventPublisher).publishOrderPlaced(any());

        // 6. Order in DB is PLACED
        Order stored = orderRepository.findById(
                be.kdg.backend.domain.order.OrderId.of(orderId.toString()))
                .orElseThrow();
        assertThat(stored.status()).isEqualTo(OrderStatus.PLACED);
    }

    @Test
    void checkoutRejectsWhenMenuValidationFails() {
        when(restaurantGateway.validateMenuItems(any())).thenReturn(
                new RestaurantGateway.MenuValidationResult(false, "Dish not published", List.of()));

        UUID customerId = UUID.randomUUID();
        Map<String, Object> cart = postJson("/api/carts", Map.of("customerId", customerId));
        UUID cartId = UUID.fromString((String) cart.get("cartId"));

        Map<String, Object> addBody = Map.of(
                "menuItemId", UUID.randomUUID(),
                "itemName", "Pizza", "quantity", 1, "unitPrice", 10.0,
                "restaurantId", knownRestaurantId);
        postJson("/api/carts/" + cartId + "/items", addBody);

        Map<String, Object> checkoutBody = Map.of(
                "cartId", cartId,
                "customerId", customerId,
                "customerName", "Ruben",
                "street", "S", "number", "1", "postalCode", "2000",
                "city", "A", "country", "BE", "email", "r@example.com");
        ResponseEntity<Map<String, Object>> resp = http.exchange(
                "/api/orders/checkout", HttpMethod.POST,
                new HttpEntity<>(checkoutBody), MAP_TYPE);
        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
    }
}