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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderReadyForPickupHandlerTest {

    @Mock DeliveryService deliveryService;

    @Test
    void handleMarksReadyForPickup() {
        OrderReadyForPickupHandler handler = new OrderReadyForPickupHandler(deliveryService);
        UUID orderId = UUID.randomUUID();
        LocalDateTime readyAt = LocalDateTime.now();
        InboundEvents.OrderReadyForPickupEvent event =
                new InboundEvents.OrderReadyForPickupEvent(orderId, UUID.randomUUID(), readyAt);

        handler.handle(event);

        verify(deliveryService).onOrderReadyForPickup(eq(be.kdg.backend.domain.shared.OrderId.of(orderId)), eq(readyAt));
    }
}
