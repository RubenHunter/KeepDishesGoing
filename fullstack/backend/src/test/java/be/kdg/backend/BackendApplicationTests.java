package be.kdg.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Smoke test — Spring context loads with H2 + AMQP auto-config excluded (test profile).
 * Mirrors {@code testing-demo/OrdersApplicationTests.contextLoads} and adds a MockitoBean for
 * RabbitTemplate because we exclude AMQP auto-config in tests (no broker).
 */
@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

    @MockitoBean
    RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {
        // Spring context must boot successfully with all our beans
    }
}