package be.kdg.backend.unit.us32;

import be.kdg.backend.application.AssignDeliveryPersonCommand;
import be.kdg.backend.application.DeliveryApplicationService;
import be.kdg.backend.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliverySingleDeliveryPersonTest {
    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private DeliveryPersonRepository deliveryPersonRepository;

    @Mock
    private DeliveryAssignmentService deliveryAssignmentService;

    @InjectMocks
    private DeliveryApplicationService deliveryService;

    //n
    @Test
    void shouldPreventAssignmentWhenDeliveryAlreadyHasDeliveryPerson() {
        DeliveryPersonId personId = DeliveryPersonId.of("DP-12345");
        DeliveryId deliveryId = DeliveryId.of("DEL-67890");
        DeliveryPersonId existingPersonId = DeliveryPersonId.of("DP-99999");

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

        delivery.assignDeliveryPerson(existingPersonId, java.time.LocalDateTime.now());

        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryPersonRepository.findById(personId)).thenReturn(Optional.of(deliveryPerson));

        doThrow(new DeliveryAlreadyAssignedException(deliveryId))
                .when(deliveryAssignmentService)
                .validateSingleDeliveryPersonPerDelivery(delivery);

        AssignDeliveryPersonCommand command = new AssignDeliveryPersonCommand(
                deliveryId.value(),
                personId.value()
        );

        assertThatThrownBy(() -> deliveryService.assignDeliveryPerson(command))
                .isInstanceOf(DeliveryAlreadyAssignedException.class);

        verify(deliveryAssignmentService).validateSingleDeliveryPersonPerDelivery(delivery);
        verify(deliveryRepository, never()).save(any());
        verify(deliveryPersonRepository, never()).save(any());
    }

    //ng
    @Test
    void shouldAllowReassignmentWhenDeliveryHasExistingDeliveryPerson() {
        DeliveryPersonId newPersonId = DeliveryPersonId.of("DP-12345");
        DeliveryPersonId currentPersonId = DeliveryPersonId.of("DP-99999");
        DeliveryId deliveryId = DeliveryId.of("DEL-67890");

        DeliveryPerson newDeliveryPerson = new DeliveryPerson(
                newPersonId,
                new PersonName("John Doe"),
                VehicleType.BICYCLE,
                true,
                new Location(52.3676, 4.9041)
        );

        DeliveryPerson currentDeliveryPerson = new DeliveryPerson(
                currentPersonId,
                new PersonName("Jane Smith"),
                VehicleType.SCOOTER,
                false,
                new Location(52.3676, 4.9041)
        );

        // Mock de Delivery om tijd-gerelateerde problemen te voorkomen
        Delivery delivery = mock(Delivery.class);
        when(delivery.getId()).thenReturn(deliveryId);
        when(delivery.getDeliveryPersonId()).thenReturn(currentPersonId);
        when(delivery.hasAssignedDeliveryPerson()).thenReturn(true);

        currentDeliveryPerson.assignDelivery(deliveryId);

        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryPersonRepository.findById(newPersonId)).thenReturn(Optional.of(newDeliveryPerson));
        when(deliveryPersonRepository.findById(currentPersonId)).thenReturn(Optional.of(currentDeliveryPerson));
        when(deliveryAssignmentService.canAssignDelivery(any(), any(), anyDouble())).thenReturn(true);

        deliveryService.reassignDeliveryPerson(new be.kdg.backend.application.ReassignDeliveryPersonCommand(
                deliveryId.value(),
                newPersonId.value()
        ));

        verify(deliveryRepository).save(delivery);
        verify(deliveryPersonRepository, times(2)).save(any());
        //Verify dat de reassignment logica werd uitgevoerd
        verify(delivery).assignDeliveryPerson(newPersonId, any(LocalDateTime.class));
    }

    @Test
    void shouldReturnTrueWhenDeliveryHasAssignedDeliveryPerson() {
        DeliveryId deliveryId = DeliveryId.of("DEL-67890");
        DeliveryPersonId personId = DeliveryPersonId.of("DP-12345");

        Delivery delivery = new Delivery(
                deliveryId,
                OrderId.of("ORD-123"),
                new Address("Street", "City", "12345", "Country"),
                new Address("Street2", "City2", "54321", "Country2")
        );

        delivery.assignDeliveryPerson(personId, java.time.LocalDateTime.now());

        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));

        boolean hasAssignedPerson = deliveryService.hasAssignedDeliveryPerson(deliveryId.value());

        assertThat(hasAssignedPerson).isTrue();
    }

    @Test
    void shouldReturnFalseWhenDeliveryHasNoAssignedDeliveryPerson() {
        DeliveryId deliveryId = DeliveryId.of("DEL-67890");

        Delivery delivery = new Delivery(
                deliveryId,
                OrderId.of("ORD-123"),
                new Address("Street", "City", "12345", "Country"),
                new Address("Street2", "City2", "54321", "Country2")
        );

        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));

        boolean hasAssignedPerson = deliveryService.hasAssignedDeliveryPerson(deliveryId.value());

        assertThat(hasAssignedPerson).isFalse();
    }
}