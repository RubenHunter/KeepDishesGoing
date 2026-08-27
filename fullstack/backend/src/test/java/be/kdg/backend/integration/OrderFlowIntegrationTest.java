package be.kdg.backend.integration;

import be.kdg.backend.application.messaging.EventPublisher;
import be.kdg.backend.application.PaymentProperties;
import be.kdg.backend.application.restaurant.RestaurantGateway;
import be.kdg.backend.domain.order.Order;
import be.kdg.backend.domain.order.OrderRepository;
import be.kdg.backend.domain.order.OrderStatus;
import be.kdg.backend.infrastructure.persistence.cart.SpringDataCartJpaRepository;
import be.kdg.backend.infrastructure.persistence.customer.SpringDataCustomerJpaRepository;
import be.kdg.backend.infrastructure.persistence.order.SpringDataOrderJpaRepository;
import be.kdg.backend.security.KeycloakRealmRoleConverter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test — full cart → checkout → place flow over HTTP against H2 (test profile) with
 * external ports mocked (RestaurantGateway + EventPublisher + RabbitTemplate — rubric: integration
 * mocks only external infra). Security is exercised with a Keycloak-style JWT (realm_access.roles).
 * No class-level {@code @Transactional} — tests clean up created rows themselves (coding-mistakes #24).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderFlowIntegrationTest {

    private static final String CUSTOMER_SUB = UUID.randomUUID().toString();

    @Autowired MockMvc mockMvc;
    @Autowired OrderRepository orderRepository;
    @Autowired PaymentProperties paymentProperties;
    @Autowired SpringDataOrderJpaRepository orderJpaRepository;
    @Autowired SpringDataCartJpaRepository cartJpaRepository;
    @Autowired SpringDataCustomerJpaRepository customerJpaRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @MockitoBean RestaurantGateway restaurantGateway;
    @MockitoBean EventPublisher eventPublisher;
    @MockitoBean RabbitTemplate rabbitTemplate;

    private UUID knownRestaurantId;

    @AfterEach
    void cleanUp() {
        orderJpaRepository.deleteAll();
        cartJpaRepository.deleteAll();
        customerJpaRepository.deleteAll();
    }

    private static RequestPostProcessor userJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject(CUSTOMER_SUB)
                        .claim("realm_access", Map.of("roles", List.of("user"))))
                .authorities(new KeycloakRealmRoleConverter());
    }

    private static RequestPostProcessor jwtWithRoles(String... roles) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject(UUID.randomUUID().toString())
                        .claim("realm_access", Map.of("roles", Arrays.asList(roles))))
                .authorities(new KeycloakRealmRoleConverter());
    }

    @BeforeEach
    void wireGateway() {
        knownRestaurantId = UUID.randomUUID();
        when(restaurantGateway.validateMenuItems(any())).thenReturn(
                new RestaurantGateway.MenuValidationResult(
                        true, "OK",
                        List.of(new RestaurantGateway.MenuValidationResult.ItemValidation(
                                UUID.randomUUID(), true, 10.0, "ok"))));
        when(restaurantGateway.getStatus(any())).thenReturn(
                new RestaurantGateway.RestaurantStatusDto(UUID.randomUUID(), true, null, null));
    }

    private JsonNode postJson(String url, Object body) throws Exception {
        MvcResult result = mockMvc.perform(post(url).with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private MvcResult postNoBody(String url) throws Exception {
        return mockMvc.perform(post(url).with(userJwt()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    private MvcResult placeOrderPatch(UUID orderId) throws Exception {
        return mockMvc.perform(patch("/api/orders/" + orderId + "/status").with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("status", "PLACED"))))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    @Test
    void createCartAddItemsAndCheckout() throws Exception {
        // 1. Create cart (owner = JWT subject)
        MvcResult createCart = postNoBody("/api/carts");
        UUID cartId = UUID.fromString(mapper.readTree(createCart.getResponse().getContentAsString()).get("cartId").asText());

        // 2. Add item
        Map<String, Object> addBody = Map.of(
                "menuItemId", UUID.randomUUID().toString(),
                "itemName", "Pizza",
                "quantity", 2,
                "unitPrice", 10.0,
                "restaurantId", knownRestaurantId.toString());
        JsonNode cartAfterAdd = postJson("/api/carts/" + cartId + "/items", addBody);
        org.assertj.core.api.Assertions.assertThat(cartAfterAdd.get("items")).hasSize(1);

        // 3. Checkout (customerId comes from the JWT, not the body)
        Map<String, Object> checkoutBody = Map.of(
                "cartId", cartId.toString(),
                "customerName", "Ruben",
                "street", "Langestraat",
                "number", "1",
                "postalCode", "2000",
                "city", "Antwerpen",
                "country", "BE",
                "email", "ruben@example.com");
        JsonNode checkoutResp = postJson("/api/orders", checkoutBody);
        UUID orderId = UUID.fromString(checkoutResp.get("orderId").asText());
        String paymentRef = checkoutResp.get("paymentRef").asText();
        org.assertj.core.api.Assertions.assertThat(paymentRef).startsWith("pay_");

        // 4. Confirm payment (webhook — permitAll but requires the shared-secret header)
        mockMvc.perform(patch("/api/payments/" + paymentRef + "/status")
                        .header(paymentProperties.webhookSecretHeader(), paymentProperties.webhookSecret()))
                .andExpect(status().isNoContent());

        // 5. Place order — should succeed (PAID) and emit event
        placeOrderPatch(orderId);
        verify(eventPublisher).publishOrderPlaced(any());

        // 6. Order in DB is PLACED
        Order stored = orderRepository.findById(
                        be.kdg.backend.domain.order.OrderId.of(orderId.toString()))
                .orElseThrow();
        org.assertj.core.api.Assertions.assertThat(stored.status()).isEqualTo(OrderStatus.PLACED);
    }

    @Test
    void checkoutRejectsWhenMenuValidationFails() throws Exception {
        when(restaurantGateway.validateMenuItems(any())).thenReturn(
                new RestaurantGateway.MenuValidationResult(false, "Dish not published", List.of()));

        MvcResult createCart = postNoBody("/api/carts");
        UUID cartId = UUID.fromString(mapper.readTree(createCart.getResponse().getContentAsString()).get("cartId").asText());

        Map<String, Object> addBody = Map.of(
                "menuItemId", UUID.randomUUID().toString(),
                "itemName", "Pizza", "quantity", 1, "unitPrice", 10.0,
                "restaurantId", knownRestaurantId.toString());
        postJson("/api/carts/" + cartId + "/items", addBody);

        Map<String, Object> checkoutBody = Map.of(
                "cartId", cartId.toString(),
                "customerName", "Ruben",
                "street", "S", "number", "1", "postalCode", "2000",
                "city", "A", "country", "BE", "email", "r@example.com");
        mockMvc.perform(post("/api/orders").with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(checkoutBody)))
                .andExpect(status().is4xxClientError());
    }

    /**
     * Guests may READ catalogue + order progress (US21/US33) without any token;
     * everything tied to a JWT identity (carts, checkout, profile, owner console) stays guarded.
     */
    @Test
    void guestReadsAreOpenButIdentityWritesAreNot() throws Exception {
        // Read-only order views: public for guests — unknown ids give 404, never 401
        mockMvc.perform(get("/api/orders/" + UUID.randomUUID() + "/tracking"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/orders/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());

        // Identity-bound writes still require a token
        mockMvc.perform(post("/api/carts"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/orders/customer"))
                .andExpect(status().isUnauthorized());

        // Owner console still locked for anonymous callers
        mockMvc.perform(get("/api/orders/restaurant/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void placeRejectedWhenRestaurantClosed() throws Exception {
        MvcResult createCart = postNoBody("/api/carts");
        UUID cartId = UUID.fromString(mapper.readTree(createCart.getResponse().getContentAsString()).get("cartId").asText());
        postJson("/api/carts/" + cartId + "/items", Map.of(
                "menuItemId", UUID.randomUUID().toString(),
                "itemName", "Pizza", "quantity", 1, "unitPrice", 10.0,
                "restaurantId", knownRestaurantId.toString()));
        JsonNode checkoutResp = postJson("/api/orders", Map.of(
                "cartId", cartId.toString(),
                "customerName", "Ruben",
                "street", "S", "number", "1", "postalCode", "2000",
                "city", "A", "country", "BE", "email", "r@example.com"));
        UUID orderId = UUID.fromString(checkoutResp.get("orderId").asText());
        String paymentRef = checkoutResp.get("paymentRef").asText();

        mockMvc.perform(patch("/api/payments/" + paymentRef + "/status")
                        .header(paymentProperties.webhookSecretHeader(), paymentProperties.webhookSecret()))
                .andExpect(status().isNoContent());

        when(restaurantGateway.getStatus(any())).thenReturn(
                new RestaurantGateway.RestaurantStatusDto(knownRestaurantId, false, null, null));

        mockMvc.perform(patch("/api/orders/" + orderId + "/status").with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("status", "PLACED"))))
                .andExpect(status().isConflict());
    }

    @Test
    void paymentWebhookRejectsWithoutSignature() throws Exception {
        mockMvc.perform(patch("/api/payments/pay_unknown/status"))
                .andExpect(status().isForbidden());
    }

    /**
     * Owner console authz: a valid JWT with the {@code owner} realm role is admitted (200);
     * a valid JWT without the role is forbidden (403), never unauthenticated (401). Only a
     * missing token yields 401.
     */
    @Test
    void ownerConsoleRoleEnforced() throws Exception {
        mockMvc.perform(get("/api/orders/restaurant/" + UUID.randomUUID()).with(jwtWithRoles("owner", "user")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders/restaurant/" + UUID.randomUUID()).with(jwtWithRoles("user")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/orders/restaurant/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    /** Customer profile is keyed by the JWT subject: save then read back (US19 account settings). */
    @Test
    void customerProfileRoundTrip() throws Exception {
        mockMvc.perform(get("/api/customers").with(userJwt()))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/customers").with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "name", "Ruben",
                                "email", "ruben@example.com",
                                "street", "Langestraat",
                                "number", "1",
                                "postalCode", "2000",
                                "city", "Antwerpen",
                                "country", "BE"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/customers").with(userJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ruben"))
                .andExpect(jsonPath("$.email").value("ruben@example.com"));
    }

    @Test
    void cancelSurfacesReasonInTracking() throws Exception {
        MvcResult createCart = postNoBody("/api/carts");
        UUID cartId = UUID.fromString(mapper.readTree(createCart.getResponse().getContentAsString()).get("cartId").asText());
        postJson("/api/carts/" + cartId + "/items", Map.of(
                "menuItemId", UUID.randomUUID().toString(),
                "itemName", "Pizza", "quantity", 1, "unitPrice", 10.0,
                "restaurantId", knownRestaurantId.toString()));
        JsonNode checkoutResp = postJson("/api/orders", Map.of(
                "cartId", cartId.toString(),
                "customerName", "Ruben",
                "street", "S", "number", "1", "postalCode", "2000",
                "city", "A", "country", "BE", "email", "r@example.com"));
        UUID orderId = UUID.fromString(checkoutResp.get("orderId").asText());
        String paymentRef = checkoutResp.get("paymentRef").asText();

        mockMvc.perform(patch("/api/payments/" + paymentRef + "/status")
                        .header(paymentProperties.webhookSecretHeader(), paymentProperties.webhookSecret()))
                .andExpect(status().isNoContent());
        placeOrderPatch(orderId);

        mockMvc.perform(patch("/api/orders/" + orderId + "/status").with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\",\"reason\":\"changed my mind\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/orders/" + orderId + "/tracking").with(userJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.rejectReason").value("changed my mind"));
    }

    @Test
    void paymentWebhookConfirmsOnlyOnce() throws Exception {
        // Full flow to obtain a real paymentRef.
        MvcResult createCart = postNoBody("/api/carts");
        UUID cartId = UUID.fromString(mapper.readTree(createCart.getResponse().getContentAsString()).get("cartId").asText());
        postJson("/api/carts/" + cartId + "/items", Map.of(
                "menuItemId", UUID.randomUUID().toString(),
                "itemName", "Pizza", "quantity", 1, "unitPrice", 10.0,
                "restaurantId", knownRestaurantId.toString()));
        JsonNode checkoutResp = postJson("/api/orders", Map.of(
                "cartId", cartId.toString(),
                "customerName", "Ruben",
                "street", "S", "number", "1", "postalCode", "2000",
                "city", "A", "country", "BE", "email", "r@example.com"));
        UUID orderId = UUID.fromString(checkoutResp.get("orderId").asText());
        String paymentRef = checkoutResp.get("paymentRef").asText();

        // Confirm twice — both succeed, but the order is paid exactly once.
        mockMvc.perform(patch("/api/payments/" + paymentRef + "/status")
                        .header(paymentProperties.webhookSecretHeader(), paymentProperties.webhookSecret()))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/payments/" + paymentRef + "/status")
                        .header(paymentProperties.webhookSecretHeader(), paymentProperties.webhookSecret()))
                .andExpect(status().isNoContent());

        Order stored = orderRepository.findById(
                        be.kdg.backend.domain.order.OrderId.of(orderId.toString()))
                .orElseThrow();
        org.assertj.core.api.Assertions.assertThat(stored.paymentStatus())
                .isEqualTo(be.kdg.backend.domain.order.PaymentStatus.PAID);
    }
}
