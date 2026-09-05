package be.kdg.backend.api;

import be.kdg.backend.application.OrderAcceptanceService;
import be.kdg.backend.application.messaging.InboundEvents;
import be.kdg.backend.application.messaging.OutboundEventPublisher;
import be.kdg.backend.application.messaging.PendingOrderStore;
import be.kdg.backend.domain.restaurant.IRestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Order-lifecycle transitions now live on one resource endpoint (mistake #16).
 * Accept must still pass the US11/US14 guard before the event is published.
 */
@ExtendWith(MockitoExtension.class)
class OrderEventControllerTest {

    @Mock OutboundEventPublisher outboundEventPublisher;
    @Mock IRestaurantRepository restaurantRepository;
    @Mock PendingOrderStore pendingOrderStore;
    @Mock OrderAcceptanceService orderAcceptanceService;

    private OrderEventController controller;
    private final UUID restaurantId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new OrderEventController(outboundEventPublisher, restaurantRepository,
                pendingOrderStore, orderAcceptanceService);
    }

    private HttpStatusCode patch(String status, String reason) {
        return controller.updateOrderStatus(restaurantId, orderId,
                new OrderEventController.OrderStatusUpdate(status, reason, null)).getStatusCode();
    }

    @Test
    void acceptedRunsGuardThenPublishesEvent() {
        assertThat(patch("ACCEPTED", null)).isEqualTo(HttpStatus.NO_CONTENT);

        verify(orderAcceptanceService).verifyCanAccept(org.mockito.ArgumentMatchers.eq(restaurantId), any());
        verify(outboundEventPublisher).publishOrderAccepted(any(InboundEvents.OrderAcceptedEvent.class));
        verify(pendingOrderStore).remove(orderId.toString());
    }

    @Test
    void rejectedPublishesReason() {
        assertThat(patch("REJECTED", "too busy")).isEqualTo(HttpStatus.NO_CONTENT);

        verify(orderAcceptanceService, never()).verifyCanAccept(any(), any());
        verify(outboundEventPublisher).publishOrderRejected(any(InboundEvents.OrderRejectedEvent.class));
    }

    @Test
    void readyForPickupPublished() {
        assertThat(patch("READY_FOR_PICKUP", null)).isEqualTo(HttpStatus.NO_CONTENT);

        verify(outboundEventPublisher).publishOrderReadyForPickup(any(InboundEvents.OrderReadyForPickupEvent.class));
    }

    @Test
    void unknownStatusRejectedBeforeAnySideEffect() {
        assertThatThrownBy(() -> patch("EAT_IT", null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(outboundEventPublisher, never()).publishOrderAccepted(any());
        verify(pendingOrderStore, never()).remove(any());
    }
}
