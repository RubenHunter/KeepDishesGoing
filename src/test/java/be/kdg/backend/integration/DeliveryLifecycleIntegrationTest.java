package be.kdg.backend.integration;

import be.kdg.backend.application.DeliveryService;
import be.kdg.backend.application.DeliveryPersonService;
import be.kdg.backend.application.messaging.OutboundEventPublisher;
import be.kdg.backend.domain.payout.PayoutRepository;
import be.kdg.backend.domain.delivery.Delivery;
import be.kdg.backend.domain.delivery.DeliveryStatus;
import be.kdg.backend.domain.driver.DeliveryPerson;
import be.kdg.backend.domain.shared.Address;
import be.kdg.backend.domain.shared.DeliveryId;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import be.kdg.backend.domain.shared.OrderId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the cross-aggregate flow when a courier delivers:
 *   Delivery saved → Spring internal event → PayoutService listener saves Payout in a separate tx
 * Also tests order-delivered AMQP events.
 */
@SpringBootTest
@ActiveProfiles("test")
class DeliveryLifecycleIntegrationTest {

    @Autowired DeliveryService deliveryService;
    @Autowired DeliveryPersonService driverService;
    @Autowired PayoutRepository payoutRepository;

    @MockitoBean RabbitTemplate rabbitTemplate;
    @MockitoBean OutboundEventPublisher outboundPublisher;

    private Delivery setupClaimedAndReadyDelivery() {
        Address pickup = new Address("R", "1", "1000", "B", "BE");
        Address deliv = new Address("K", "2", "1000", "B", "BE");
        Delivery d = deliveryService.onOrderAccepted(
                OrderId.of(UUID.randomUUID()), pickup, deliv);
        DeliveryPersonId driver = driverService.registerDriver("Ruben", "BICYCLE");
        deliveryService.selfAssignDelivery(d.id(), driver, LocalDateTime.now());
        deliveryService.onOrderReadyForPickup(d.orderId(), LocalDateTime.now());
        deliveryService.markPickedUp(d.id(), LocalDateTime.now());
        // Simulate pickup time = 10 min ago, ready = 12 min ago so billable is 10 (within window)
        return d;
    }

    @Test
    void deliverCreatesPayoutAndPublishesAmqpEvent() {
        Delivery d = setupClaimedAndReadyDelivery();

        deliveryService.markDelivered(d.id(), LocalDateTime.now());

        // AMQP called
        org.mockito.Mockito.verify(outboundPublisher).publishDelivered(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        // Payout was created in a separate tx via @TransactionalEventListener
        assertThat(payoutRepository.findByDeliveryId(d.id())).isPresent();
        Delivery after = deliveryService.get(d.id());
        assertThat(after.status()).isEqualTo(DeliveryStatus.DELIVERED);
    }
}