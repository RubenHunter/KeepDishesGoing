package be.kdg.backend.unit.us27;

import be.kdg.backend.application.DeliveryApplicationService;
import be.kdg.backend.application.ListAvailableDeliveriesCommand;
import be.kdg.backend.application.SelfAssignDeliveryCommand;
import be.kdg.backend.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliverySelfAssignmentTest {
    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private DeliveryPersonRepository deliveryPersonRepository;

    @Mock
    private DeliveryAssignmentService deliveryAssignmentService;

    @InjectMocks
    private DeliveryApplicationService deliveryService;

    @Test
    void shouldAllowDeliveryPersonToSelfAssignToAvailableDelivery() {
        DeliveryPersonId personId = DeliveryPersonId.of("DP-12345");
        DeliveryId deliveryId = DeliveryId.of("DEL-67890");

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

        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryPersonRepository.findById(personId)).thenReturn(Optional.of(deliveryPerson));
        when(deliveryAssignmentService.canSelfAssignDelivery(deliveryPerson, delivery, 10.0)).thenReturn(true);

        SelfAssignDeliveryCommand command = new SelfAssignDeliveryCommand(
                deliveryId.value(),
                personId.value()
        );

        deliveryService.selfAssignDelivery(command);

        verify(deliveryRepository).save(delivery);
        verify(deliveryPersonRepository).save(deliveryPerson);
        assertThat(delivery.hasAssignedDeliveryPerson()).isTrue();
        assertThat(delivery.getDeliveryPersonId()).isEqualTo(personId);
        assertThat(delivery.isAvailableForSelfAssignment()).isFalse();
    }

    //ng
    @Test
    void shouldPreventSelfAssignmentWhenDeliveryPersonAlreadyAssigned() {
        DeliveryPersonId personId = DeliveryPersonId.of("DP-12345");
        DeliveryId deliveryId = DeliveryId.of("DEL-67890");

        DeliveryPerson deliveryPerson = new DeliveryPerson(
                personId,
                new PersonName("John Doe"),
                VehicleType.BICYCLE,
                false,
                new Location(52.3676, 4.9041)
        );

        Delivery delivery = new Delivery(
                deliveryId,
                OrderId.of("ORD-123"),
                new Address("Street", "City", "12345", "Country"),
                new Address("Street2", "City2", "54321", "Country2")
        );

        deliveryPerson.assignDelivery(DeliveryId.of("DEL-OTHER"));

        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryPersonRepository.findById(personId)).thenReturn(Optional.of(deliveryPerson));

        doThrow(new DeliveryPersonAlreadyAssignedException(personId))
                .when(deliveryAssignmentService)
                .validateSingleActiveAssignment(deliveryPerson);

        SelfAssignDeliveryCommand command = new SelfAssignDeliveryCommand(
                deliveryId.value(),
                personId.value()
        );

        assertThatThrownBy(() -> deliveryService.selfAssignDelivery(command))
                .isInstanceOf(DeliveryPersonAlreadyAssignedException.class);

        verify(deliveryRepository, never()).save(any());
        verify(deliveryPersonRepository, never()).save(any());
    }

    @Test
    void shouldPreventSelfAssignmentWhenDeliveryNotAvailable() {
        DeliveryPersonId personId = DeliveryPersonId.of("DP-12345");
        DeliveryId deliveryId = DeliveryId.of("DEL-67890");

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

        delivery.markAsUnavailableForAssignment();

        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryPersonRepository.findById(personId)).thenReturn(Optional.of(deliveryPerson));

        doThrow(new DeliveryNotAvailableException(deliveryId))
                .when(deliveryAssignmentService)
                .validateDeliveryAvailableForAssignment(delivery);

        SelfAssignDeliveryCommand command = new SelfAssignDeliveryCommand(
                deliveryId.value(),
                personId.value()
        );

        assertThatThrownBy(() -> deliveryService.selfAssignDelivery(command))
                .isInstanceOf(DeliveryNotAvailableException.class);

        verify(deliveryRepository, never()).save(any());
        verify(deliveryPersonRepository, never()).save(any());
    }

    @Test
    void shouldReturnAvailableDeliveriesForSelfAssignment() {
        DeliveryPersonId personId = DeliveryPersonId.of("DP-12345");

        DeliveryPerson deliveryPerson = new DeliveryPerson(
                personId,
                new PersonName("John Doe"),
                VehicleType.BICYCLE,
                true,
                new Location(52.3676, 4.9041)
        );

        Delivery delivery1 = new Delivery(
                DeliveryId.of("DEL-11111"),
                OrderId.of("ORD-111"),
                new Address("Street1", "City1", "11111", "Country1"),
                new Address("Street1a", "City1a", "11112", "Country1")
        );

        Delivery delivery2 = new Delivery(
                DeliveryId.of("DEL-22222"),
                OrderId.of("ORD-222"),
                new Address("Street2", "City2", "22222", "Country2"),
                new Address("Street2a", "City2a", "22223", "Country2")
        );

        when(deliveryPersonRepository.findById(personId)).thenReturn(Optional.of(deliveryPerson));
        when(deliveryRepository.findAvailableForAssignment()).thenReturn(List.of(delivery1, delivery2));

        ListAvailableDeliveriesCommand command = new ListAvailableDeliveriesCommand(
                personId.value(), 52.3676, 4.9041, 10.0
        );

        var availableDeliveries = deliveryService.getAvailableDeliveriesForSelfAssignment(command);

        assertThat(availableDeliveries).hasSize(2);
        verify(deliveryRepository).findAvailableForAssignment();
    }
}