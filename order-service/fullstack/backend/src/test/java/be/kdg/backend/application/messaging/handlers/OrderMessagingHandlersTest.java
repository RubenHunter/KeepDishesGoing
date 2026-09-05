package be.kdg.backend.application.messaging.handlers;

import be.kdg.backend.application.OrderService;
import be.kdg.backend.application.messaging.InboundEvents;
import be.kdg.backend.application.tracking.TrackingService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderMessagingHandlersTest {

    @Mock OrderService orderService;
    @Mock TrackingService trackingService;

    private final UUID orderId = UUID.randomUUID();

    @Test
    void acceptedHandlerRecordsAndDelegates() {
        OrderAcceptedHandler handler = new OrderAcceptedHandler(orderService, trackingService);
        InboundEvents.OrderAcceptedEvent event = new InboundEvents.OrderAcceptedEvent(
                orderId, UUID.randomUUID(), "Langestraat 1", LocalDateTime.now());

        handler.handle(event);

        verify(trackingService).recordEvent(eq(orderId), eq("ORDER_ACCEPTED"), anyString());
        verify(orderService).onOrderAccepted(eq(orderId), any());
    }

    @Test
    void rejectedHandlerRecordsAndDelegates() {
        OrderRejectedHandler handler = new OrderRejectedHandler(orderService, trackingService);
        InboundEvents.OrderRejectedEvent event = new InboundEvents.OrderRejectedEvent(
                orderId, "out of stock", LocalDateTime.now());

        handler.handle(event);

        verify(trackingService).recordEvent(eq(orderId), eq("ORDER_REJECTED"), anyString());
        verify(orderService).onOrderRejected(eq(orderId), eq("out of stock"), any());
    }

    @Test
    void readyForPickupHandlerRecordsAndDelegates() {
        OrderReadyForPickupHandler handler = new OrderReadyForPickupHandler(orderService, trackingService);
        InboundEvents.OrderReadyForPickupEvent event = new InboundEvents.OrderReadyForPickupEvent(
                orderId, UUID.randomUUID(), LocalDateTime.now());

        handler.handle(event);

        verify(trackingService).recordEvent(eq(orderId), eq("ORDER_READY_FOR_PICKUP"), anyString());
        verify(orderService).onOrderReadyForPickup(eq(orderId));
    }

    @Test
    void pickedUpHandlerRecordsAndDelegates() {
        OrderPickedUpHandler handler = new OrderPickedUpHandler(orderService, trackingService);
        InboundEvents.OrderPickedUpEvent event = new InboundEvents.OrderPickedUpEvent(
                orderId, UUID.randomUUID(), LocalDateTime.now());

        handler.handle(event);

        verify(trackingService).recordEvent(eq(orderId), eq("ORDER_PICKED_UP"), anyString());
        verify(orderService).onOrderPickedUp(eq(orderId));
    }

    @Test
    void deliveredHandlerRecordsAndDelegates() {
        OrderDeliveredHandler handler = new OrderDeliveredHandler(orderService, trackingService);
        InboundEvents.OrderDeliveredEvent event = new InboundEvents.OrderDeliveredEvent(
                orderId, UUID.randomUUID(), LocalDateTime.now());

        handler.handle(event);

        verify(trackingService).recordEvent(eq(orderId), eq("ORDER_DELIVERED"), anyString());
        verify(orderService).onOrderDelivered(eq(orderId));
    }

    @Test
    void handlersTolerateNullTimestamps() {
        OrderAcceptedHandler handler = new OrderAcceptedHandler(orderService, trackingService);
        InboundEvents.OrderAcceptedEvent event = new InboundEvents.OrderAcceptedEvent(
                orderId, UUID.randomUUID(), null, null);

        handler.handle(event);

        verify(orderService).onOrderAccepted(eq(orderId), isNull());
    }
}
