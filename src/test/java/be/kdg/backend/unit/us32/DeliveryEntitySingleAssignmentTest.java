package be.kdg.backend.unit.us32;

import be.kdg.backend.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryEntitySingleAssignmentTest {
    private Delivery delivery;
    private DeliveryPersonId personId1;
    private DeliveryPersonId personId2;

    @BeforeEach
    void setUp() {
        delivery = new Delivery(
                DeliveryId.generate(),
                OrderId.of("ORD-123"),
                new Address("Street", "City", "12345", "Country"),
                new Address("Street2", "City2", "54321", "Country2")
        );
        personId1 = DeliveryPersonId.generate();
        personId2 = DeliveryPersonId.generate();
    }

    @Test
    void shouldAssignDeliveryPersonWhenNoExistingAssignment() {
        // Gebruik toekomstige tijd om validatie te omzeilen
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(1);
        delivery.assignDeliveryPerson(personId1, futureTime);

        assertThat(delivery.hasAssignedDeliveryPerson()).isTrue();
        assertThat(delivery.getDeliveryPersonId()).isEqualTo(personId1);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
    }

    @Test
    void shouldThrowExceptionWhenAssigningSecondDeliveryPerson() {
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(1);
        delivery.assignDeliveryPerson(personId1, futureTime);

        assertThatThrownBy(() -> delivery.assignDeliveryPerson(personId2, futureTime.plusMinutes(1)))
                .isInstanceOf(DeliveryAlreadyAssignedException.class)
                .hasMessageContaining("Delivery already has an assigned delivery person");
    }

    @Test
    void shouldClearDeliveryPersonWhenMarkedDelivered() {
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(1);
        delivery.assignDeliveryPerson(personId1, futureTime);
        delivery.markPickedUp();
        delivery.markDelivered();

        assertThat(delivery.hasAssignedDeliveryPerson()).isFalse();
        assertThat(delivery.getDeliveryPersonId()).isNull();
    }

    @Test
    void shouldClearDeliveryPersonWhenCancelled() {
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(1);
        delivery.assignDeliveryPerson(personId1, futureTime);
        delivery.cancelDelivery(new CancellationReason("Customer cancelled"));

        assertThat(delivery.hasAssignedDeliveryPerson()).isFalse();
        assertThat(delivery.getDeliveryPersonId()).isNull();
    }

    @Test
    void shouldReturnCorrectAssignedDeliveryPersonId() {
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(1);
        delivery.assignDeliveryPerson(personId1, futureTime);

        assertThat(delivery.getAssignedDeliveryPersonId()).isEqualTo(personId1);
    }
}