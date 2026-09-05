package be.kdg.backend.application.messaging;

import be.kdg.backend.application.messaging.EventPublisher.OrderPlacedEvent;
import be.kdg.backend.application.messaging.EventPublisher.OrderPlacedEvent.OrderPlacedItem;
import be.kdg.backend.application.messaging.handlers.OrderPlacedHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the order.placed consumer (US22) — delivery address is stored
 * so the owner's accept action can resolve it later.
 */
@ExtendWith(MockitoExtension.class)
class OrderPlacedHandlerTest {

    @Mock PendingOrderStore pendingOrderStore;

    private OrderPlacedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrderPlacedHandler(pendingOrderStore);
    }

    private OrderPlacedEvent event(String orderId, String deliveryAddress) {
        return new OrderPlacedEvent(
                orderId,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                deliveryAddress,
                List.of(new OrderPlacedItem(UUID.randomUUID().toString(), "Dish", 2, 12.5)),
                LocalDateTime.of(2026, 1, 1, 12, 0));
    }

    @Test
    void storesDeliveryAddressForPendingDecision() {
        String orderId = UUID.randomUUID().toString();
        handler.handle(event(orderId, "Street 1, 2000 Antwerp"));

        verify(pendingOrderStore).put(orderId, "Street 1, 2000 Antwerp");
    }

    @Test
    void blankDeliveryAddressIsNotStored() {
        String orderId = UUID.randomUUID().toString();
        handler.handle(event(orderId, ""));

        verify(pendingOrderStore, never()).put(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void nullDeliveryAddressIsNotStored() {
        String orderId = UUID.randomUUID().toString();
        handler.handle(event(orderId, null));

        verify(pendingOrderStore, never()).put(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }
}
