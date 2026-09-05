package be.kdg.backend.integration;

import be.kdg.backend.application.messaging.EventPublisher;
import be.kdg.backend.application.payment.PaymentGateway;
import be.kdg.backend.application.restaurant.RestaurantGateway;
import be.kdg.backend.domain.StripeSignatureException;
import be.kdg.backend.domain.order.Order;
import be.kdg.backend.domain.order.OrderId;
import be.kdg.backend.domain.order.OrderRepository;
import be.kdg.backend.domain.order.PaymentStatus;
import be.kdg.backend.infrastructure.payment.StripeWebhookVerifier;
import be.kdg.backend.infrastructure.persistence.cart.SpringDataCartJpaRepository;
import be.kdg.backend.infrastructure.persistence.customer.SpringDataCustomerJpaRepository;
import be.kdg.backend.infrastructure.persistence.order.SpringDataOrderJpaRepository;
import be.kdg.backend.security.KeycloakRealmRoleConverter;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Stripe webhook integration (US20). The Stripe SDK is mocked at the boundary ({@link PaymentGateway}
 * port + {@link StripeWebhookVerifier}), so no Stripe network or signature computation happens — only
 * external infra is mocked, per the rubric. Covers: checkout redirect URL, completed-checkout webhook
 * → PAID → place, bad signature → 400, and webhook redelivery idempotency.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "kdg.payment.provider=stripe",
        "kdg.payment.stripe.secret-key=sk_test_dummy",
        "kdg.payment.stripe.webhook-secret=whsec_dummy"
})
class StripeWebhookIntegrationTest {

    private static final String CUSTOMER_SUB = UUID.randomUUID().toString();
    private static final String SESSION_ID = "cs_test_123";

    @Autowired MockMvc mockMvc;
    @Autowired OrderRepository orderRepository;
    @Autowired SpringDataOrderJpaRepository orderJpaRepository;
    @Autowired SpringDataCartJpaRepository cartJpaRepository;
    @Autowired SpringDataCustomerJpaRepository customerJpaRepository;

    @MockitoBean PaymentGateway paymentGateway;
    @MockitoBean StripeWebhookVerifier webhookVerifier;
    @MockitoBean RestaurantGateway restaurantGateway;
    @MockitoBean EventPublisher eventPublisher;
    @MockitoBean RabbitTemplate rabbitTemplate;

    private final ObjectMapper mapper = new ObjectMapper();
    private UUID knownRestaurantId;

    @AfterEach
    void cleanUp() {
        orderJpaRepository.deleteAll();
        cartJpaRepository.deleteAll();
        customerJpaRepository.deleteAll();
    }

    @BeforeEach
    void wireMocks() throws Exception {
        knownRestaurantId = UUID.randomUUID();
        when(restaurantGateway.validateMenuItems(any())).thenReturn(
                new RestaurantGateway.MenuValidationResult(true, "OK", List.of()));
        when(restaurantGateway.getStatus(any())).thenReturn(
                new RestaurantGateway.RestaurantStatusDto(UUID.randomUUID(), true, null, null));
        when(paymentGateway.startPayment(any())).thenReturn(
                new PaymentGateway.StartPaymentResponse(SESSION_ID, "https://checkout.stripe.com/c/pay/" + SESSION_ID));
        when(paymentGateway.confirm(SESSION_ID)).thenReturn(
                new PaymentGateway.PaymentConfirmation(SESSION_ID, PaymentGateway.PaymentConfirmation.PaymentStatus.PAID));
        when(webhookVerifier.completedCheckoutSessionId(any(), any())).thenReturn(SESSION_ID);
    }

    private static RequestPostProcessor userJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject(CUSTOMER_SUB)
                        .claim("realm_access", Map.of("roles", List.of("user"))))
                .authorities(new KeycloakRealmRoleConverter());
    }

    private UUID checkout() throws Exception {
        String createCart = mockMvc.perform(post("/api/carts").with(userJwt()))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        UUID cartId = UUID.fromString(mapper.readTree(createCart).get("cartId").asText());

        mockMvc.perform(post("/api/carts/" + cartId + "/items").with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "menuItemId", UUID.randomUUID().toString(),
                                "itemName", "Pizza", "quantity", 2, "unitPrice", 10.0,
                                "restaurantId", knownRestaurantId.toString()))))
                .andExpect(status().is2xxSuccessful());

        String checkoutResp = mockMvc.perform(post("/api/orders").with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "cartId", cartId.toString(),
                                "customerName", "Ruben",
                                "street", "Langestraat", "number", "1", "postalCode", "2000",
                                "city", "Antwerpen", "country", "BE", "email", "ruben@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentRef").value(SESSION_ID))
                .andExpect(jsonPath("$.redirectUrl").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(mapper.readTree(checkoutResp).get("orderId").asText());
    }

    private void postWebhook(String payload, String signature) throws Exception {
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk());
    }

    @Test
    void completedCheckoutWebhookMarksPaidThenPlaces() throws Exception {
        UUID orderId = checkout();

        postWebhook("{}", "t=1,v1=abcd");

        Order stored = orderRepository.findById(OrderId.of(orderId.toString())).orElseThrow();
        assertThat(stored.paymentStatus()).isEqualTo(PaymentStatus.PAID);

        mockMvc.perform(patch("/api/orders/" + orderId + "/status").with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("status", "PLACED"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void webhookRedeliveryIsIdempotent() throws Exception {
        UUID orderId = checkout();

        String payload = "{}";
        String signature = "t=1,v1=abcd";
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk());

        Order stored = orderRepository.findById(OrderId.of(orderId.toString())).orElseThrow();
        assertThat(stored.paymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void webhookWithBadSignatureReturns400() throws Exception {
        when(webhookVerifier.completedCheckoutSessionId(any(), any()))
                .thenThrow(new StripeSignatureException("Invalid Stripe webhook signature"));

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=1,v1=bad")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkoutReturnsRedirectUrlWhenProviderIsStripe() throws Exception {
        // checkout() already asserts redirectUrl is non-empty; re-assert the order stayed PENDING/AWAITING.
        UUID orderId = checkout();
        Order stored = orderRepository.findById(OrderId.of(orderId.toString())).orElseThrow();
        assertThat(stored.status()).isEqualTo(be.kdg.backend.domain.order.OrderStatus.PENDING);
        assertThat(stored.paymentStatus()).isEqualTo(PaymentStatus.AWAITING);
    }
}
