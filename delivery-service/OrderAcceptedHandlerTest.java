package be.kdg.backend.application.messaging.handlers;

import be.kdg.backend.application.DeliveryService;
import be.kdg.backend.application.messaging.InboundEvents;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderAcceptedHandlerTest {

    @Mock DeliveryService deliveryService;

    @Test
    void handleCreatesDeliveryFromEvent() {
        OrderAcceptedHandler handler = new OrderAcceptedHandler(deliveryService);
        InboundEvents.OrderAcceptedEvent event = new InboundEvents.OrderAcceptedEvent(
                UUID.randomUUID(), UUID.randomUUID(),
                "Langestraat 12, 2000 Antwerpen, BE",
                "Keyserlei 1, 2018 Antwerpen, BE",
                LocalDateTime.now());

        handler.handle(event);

        verify(deliveryService).onOrderAccepted(any(), any(), any());
    }

    @Test
    void handleWithBlankAddressStillCreatesDelivery() {
        OrderAcceptedHandler handler = new OrderAcceptedHandler(deliveryService);
        InboundEvents.OrderAcceptedEvent event = new InboundEvents.OrderAcceptedEvent(
                UUID.randomUUID(), UUID.randomUUID(), null, null, LocalDateTime.now());

        handler.handle(event);

        verify(deliveryService).onOrderAccepted(any(), any(), any());
    }
}
