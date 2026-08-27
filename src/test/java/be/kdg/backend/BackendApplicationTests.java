package be.kdg.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Smoke test — delivery-service Spring context loads with H2 + AMQP auto-config excluded (test profile).
 */
@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

    @MockitoBean
    RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {
        // Verifies every bean wires without RabbitMQ broker
    }
}