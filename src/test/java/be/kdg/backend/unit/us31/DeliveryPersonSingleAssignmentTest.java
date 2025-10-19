package be.kdg.backend.unit.us31;

import be.kdg.backend.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryPersonSingleAssignmentTest {
    private DeliveryPerson deliveryPerson;
    private DeliveryId deliveryId1;
    private DeliveryId deliveryId2;

    @BeforeEach
    void setUp() {
        deliveryPerson = new DeliveryPerson(
                DeliveryPersonId.generate(),
                new PersonName("John Doe"),
                VehicleType.BICYCLE,
                true,
                new Location(52.3676, 4.9041)
        );
        deliveryId1 = DeliveryId.generate();
        deliveryId2 = DeliveryId.generate();
    }

    @Test
    void shouldAssignDeliveryWhenNoActiveAssignment() {
        deliveryPerson.assignDelivery(deliveryId1);

        assertThat(deliveryPerson.hasActiveAssignment()).isTrue();
        assertThat(deliveryPerson.getAssignedDeliveryId()).isEqualTo(deliveryId1);
        assertThat(deliveryPerson.isAvailable()).isFalse();
    }

    @Test
    void shouldThrowExceptionWhenAssigningSecondDelivery() {
        deliveryPerson.assignDelivery(deliveryId1);

        assertThatThrownBy(() -> deliveryPerson.assignDelivery(deliveryId2))
                .isInstanceOf(DeliveryPersonAlreadyAssignedException.class)
                .hasMessageContaining("Delivery person already has an active assignment");
    }

    @Test
    void shouldAllowNewAssignmentAfterUnassigning() {
        deliveryPerson.assignDelivery(deliveryId1);
        deliveryPerson.unassignDelivery();
        deliveryPerson.assignDelivery(deliveryId2);

        assertThat(deliveryPerson.getAssignedDeliveryId()).isEqualTo(deliveryId2);
    }

    @Test
    void shouldThrowExceptionWhenSettingUnavailableWithActiveAssignment() {
        deliveryPerson.assignDelivery(deliveryId1);

        assertThatThrownBy(() -> deliveryPerson.updateAvailability(false))
                .isInstanceOf(DeliveryPersonAlreadyAssignedException.class);
    }

    @Test
    void shouldReturnFalseForCanAcceptDeliveryWhenAssigned() {
        deliveryPerson.assignDelivery(deliveryId1);

        assertThat(deliveryPerson.canAcceptDelivery()).isFalse();
    }

    @Test
    void shouldReturnTrueForCanAcceptDeliveryWhenUnassigned() {
        assertThat(deliveryPerson.canAcceptDelivery()).isTrue();
    }
}