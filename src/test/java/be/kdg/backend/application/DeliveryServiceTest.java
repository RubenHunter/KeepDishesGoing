package be.kdg.backend.application;

import be.kdg.backend.application.messaging.InternalEvents;
import be.kdg.backend.application.messaging.OutboundEventPublisher;
import be.kdg.backend.domain.delivery.Delivery;
import be.kdg.backend.domain.delivery.DeliveryRepository;
import be.kdg.backend.domain.shared.Address;
import be.kdg.backend.domain.shared.DeliveryId;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import be.kdg.backend.domain.shared.OrderId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock DeliveryRepository deliveryRepository;
    @Mock ApplicationEventPublisher springEvents;
    @Mock OutboundEventPublisher outbound;

    private DeliveryService sut;

    private final LocalDateTime now = LocalDateTime.of(2024, 1, 1, 12, 0);

    @BeforeEach
    void setUp() {
        sut = new DeliveryService(deliveryRepository, springEvents, outbound);
    }

    private static Address addr(String s) {
        return new Address(s, "1", "1000", "City", "BE");
    }

    @Test
    void onOrderAcceptedCreatesDeliveryWhenNew() {
        OrderId orderId = OrderId.of(UUID.randomUUID());
        given(deliveryRepository.findByOrderId(orderId)).willReturn(Optional.empty());
        given(deliveryRepository.save(any())).willAnswer(i -> i.getArgument(0));

        Delivery result = sut.onOrderAccepted(orderId, addr("P"), addr("D"));

        assertThat(result.orderId()).isEqualTo(orderId);
        verify(deliveryRepository).save(any());
    }

    @Test
    void onOrderAcceptedReturnsExistingDeliveryOnDuplicate() {
        OrderId orderId = OrderId.of(UUID.randomUUID());
        Delivery existing = new Delivery(DeliveryId.generate(), orderId, addr("P"), addr("D"));
        given(deliveryRepository.findByOrderId(orderId)).willReturn(Optional.of(existing));

        Delivery result = sut.onOrderAccepted(orderId, addr("P"), addr("D"));

        assertThat(result).isSameAs(existing);
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void selfAssignDeliveryAssignsAndPublishesEvent() {
        Delivery d = new Delivery(DeliveryId.generate(), OrderId.of(UUID.randomUUID()), addr("P"), addr("D"));
        DeliveryPersonId driver = DeliveryPersonId.generate();
        given(deliveryRepository.findById(d.id())).willReturn(Optional.of(d));

        sut.selfAssignDelivery(d.id(), driver, now);

        verify(deliveryRepository).save(d);
        verify(springEvents).publishEvent(any(InternalEvents.CourierAssignedEvent.class));
    }

    @Test
    void cancelClaimReleasesDriver() {
        Delivery d = new Delivery(DeliveryId.generate(), OrderId.of(UUID.randomUUID()), addr("P"), addr("D"));
        DeliveryPersonId driver = DeliveryPersonId.generate();
        d.selfAssign(driver, now);
        given(deliveryRepository.findById(d.id())).willReturn(Optional.of(d));

        sut.cancelClaim(d.id(), driver, "changed mind", now);

        verify(deliveryRepository).save(d);
        ArgumentCaptor<InternalEvents.CourierReleasedEvent> cap = ArgumentCaptor.forClass(InternalEvents.CourierReleasedEvent.class);
        verify(springEvents).publishEvent(cap.capture());
        assertThat(cap.getValue().driverId()).isEqualTo(driver);
    }

    @Test
    void markPickedUpPublishesPickedUpEvent() {
        Delivery d = new Delivery(DeliveryId.generate(), OrderId.of(UUID.randomUUID()), addr("P"), addr("D"));
        DeliveryPersonId driver = DeliveryPersonId.generate();
        d.selfAssign(driver, now);
        d.onOrderReadyForPickup(now);
        given(deliveryRepository.findById(d.id())).willReturn(Optional.of(d));

        sut.markPickedUp(d.id(), driver, now);

        verify(deliveryRepository).save(d);
        verify(outbound).publishPickedUp(d.orderId().value(), d.id().value(), now);
    }

    @Test
    void markDeliveredPublishesDeliveredAndInternalEvents() {
        Delivery d = new Delivery(DeliveryId.generate(), OrderId.of(UUID.randomUUID()), addr("P"), addr("D"));
        DeliveryPersonId driver = DeliveryPersonId.generate();
        d.selfAssign(driver, now);
        d.onOrderReadyForPickup(now);
        d.markPickedUp(now);
        d.markInTransit(now);
        given(deliveryRepository.findById(d.id())).willReturn(Optional.of(d));

        sut.markDelivered(d.id(), driver, now);

        verify(deliveryRepository).save(d);
        verify(outbound).publishDelivered(d.orderId().value(), d.id().value(), now);
        verify(springEvents).publishEvent(any(InternalEvents.DeliveryDeliveredEvent.class));
        verify(springEvents).publishEvent(any(InternalEvents.CourierReleasedEvent.class));
    }

    @Test
    void listAvailableDelegatesToRepository() {
        given(deliveryRepository.findAvailableForSelfAssignment()).willReturn(List.of());
        assertThat(sut.listAvailable()).isEmpty();
    }

    @Test
    void listForDriverDelegatesToRepository() {
        DeliveryPersonId driver = DeliveryPersonId.generate();
        given(deliveryRepository.findByDeliveryPersonId(driver)).willReturn(List.of());
        assertThat(sut.listForDriver(driver)).isEmpty();
    }
    @Test
    void markPickedUpRejectedForOtherDriver() {
        Delivery d = new Delivery(DeliveryId.generate(), OrderId.of(UUID.randomUUID()), addr("P"), addr("D"));
        DeliveryPersonId owner = DeliveryPersonId.generate();
        DeliveryPersonId intruder = DeliveryPersonId.generate();
        d.selfAssign(owner, now);
        d.onOrderReadyForPickup(now);
        given(deliveryRepository.findById(d.id())).willReturn(Optional.of(d));

        org.junit.jupiter.api.Assertions.assertThrows(
                be.kdg.backend.domain.delivery.DeliveryOwnershipException.class,
                () -> sut.markPickedUp(d.id(), intruder, now));
        verify(deliveryRepository, never()).save(any());
        verify(outbound, never()).publishPickedUp(any(), any(), any());
    }

    @Test
    void cancelClaimRequiresAssignedCourier() {
        Delivery d = new Delivery(DeliveryId.generate(), OrderId.of(UUID.randomUUID()), addr("P"), addr("D"));
        given(deliveryRepository.findById(d.id())).willReturn(Optional.of(d));

        org.junit.jupiter.api.Assertions.assertThrows(
                be.kdg.backend.domain.delivery.DeliveryOwnershipException.class,
                () -> sut.cancelClaim(d.id(), DeliveryPersonId.generate(), "no claim", now));
        verify(deliveryRepository, never()).save(any());
    }
}