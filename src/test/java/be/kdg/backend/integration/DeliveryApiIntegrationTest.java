package be.kdg.backend.integration;

import be.kdg.backend.application.DeliveryService;
import be.kdg.backend.application.messaging.OutboundEventPublisher;
import be.kdg.backend.domain.shared.Address;
import be.kdg.backend.domain.shared.OrderId;
import be.kdg.backend.infrastructure.security.KeycloakRealmRoleConverter;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the courier-facing API (DeliveryController + DriverController) over MockMvc with a
 * Keycloak-style driver JWT, against H2 (test profile). Covers the controller + DTO layers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeliveryApiIntegrationTest {

    private static final String DRIVER_SUB = UUID.randomUUID().toString();

    @Autowired MockMvc mockMvc;
    @Autowired DeliveryService deliveryService;

    @MockitoBean RabbitTemplate rabbitTemplate;
    @MockitoBean OutboundEventPublisher outboundPublisher;

    private static RequestPostProcessor driverJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject(DRIVER_SUB)
                        .claim("realm_access", Map.of("roles", List.of("driver"))))
                .authorities(new KeycloakRealmRoleConverter());
    }

    @Test
    void unauthenticatedAccessIsRejected() throws Exception {
        mockMvc.perform(get("/api/deliveries/available"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void driverRegistersListsAndClaimsDelivery() throws Exception {
        // Register driver (id = JWT subject)
        mockMvc.perform(post("/api/drivers").with(driverJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ruben\",\"vehicle\":\"BICYCLE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverId").value(DRIVER_SUB));

        // A delivery becomes claimable (order.accepted consumed)
        var delivery = deliveryService.onOrderAccepted(
                OrderId.of(UUID.randomUUID()),
                new Address("Langestraat", "12", "2000", "Antwerpen", "BE"),
                new Address("Keyserlei", "1", "2018", "Antwerpen", "BE"));

        // List available
        mockMvc.perform(get("/api/deliveries/available").with(driverJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deliveryId").value(delivery.id().value().toString()));

        // Claim
        mockMvc.perform(post("/api/deliveries/" + delivery.id().value() + "/claim").with(driverJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\":\"" + DRIVER_SUB + "\"}"))
                .andExpect(status().isOk());

        // Get detail
        mockMvc.perform(get("/api/deliveries/" + delivery.id().value()).with(driverJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        // My deliveries
        mockMvc.perform(get("/api/deliveries?driverId=" + DRIVER_SUB).with(driverJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deliveryId").value(delivery.id().value().toString()));

        // Payouts (guard: self-only)
        mockMvc.perform(get("/api/drivers/" + DRIVER_SUB + "/payouts").with(driverJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverId").value(DRIVER_SUB));
    }
}
