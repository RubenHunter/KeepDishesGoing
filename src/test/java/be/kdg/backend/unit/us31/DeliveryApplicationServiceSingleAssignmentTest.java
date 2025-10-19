package be.kdg.backend.unit.us31;
import be.kdg.backend.application.AssignDeliveryPersonCommand;
import be.kdg.backend.application.DeliveryApplicationService;
import be.kdg.backend.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryApplicationServiceSingleAssignmentTest {
    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private DeliveryPersonRepository deliveryPersonRepository;

    @Mock
    private DeliveryAssignmentService deliveryAssignmentService;

    @InjectMocks
    private DeliveryApplicationService deliveryService;

    @Test
    void shouldPreventAssignmentWhenDeliveryPersonAlreadyAssigned() {
        // Specifieke IDs met correcte format
        DeliveryPersonId personId = DeliveryPersonId.of("DP-12345");
        DeliveryId deliveryId = DeliveryId.of("DEL-67890");
        DeliveryId otherDeliveryId = DeliveryId.of("DEL-11111");

        DeliveryPerson deliveryPerson = new DeliveryPerson(
                personId,
                new PersonName("John Doe"),
                VehicleType.BICYCLE,
                true,
                new Location(52.3676, 4.9041)
        );

        Delivery delivery = new Delivery(
                deliveryId,
                OrderId.of("ORD-123"),
                new Address("Street", "City", "12345", "Country"),
                new Address("Street2", "City2", "54321", "Country2")
        );

        // Simuleer dat de delivery person al een assignment heeft
        deliveryPerson.assignDelivery(otherDeliveryId);

        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryPersonRepository.findById(personId)).thenReturn(Optional.of(deliveryPerson));

        AssignDeliveryPersonCommand command = new AssignDeliveryPersonCommand(
                deliveryId.value(),
                personId.value()
        );

        assertThatThrownBy(() -> deliveryService.assignDeliveryPerson(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Delivery person cannot be assigned to this delivery");

        // Verify dat canAssignDelivery werd aangeroepen en false returned
        verify(deliveryAssignmentService).canAssignDelivery(deliveryPerson, delivery, 10.0);
        verify(deliveryRepository, never()).save(any());
        verify(deliveryPersonRepository, never()).save(any());
    }
}