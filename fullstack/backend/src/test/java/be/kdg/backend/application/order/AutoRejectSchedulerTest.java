package be.kdg.backend.application.order;

import be.kdg.backend.application.OrderService;
import be.kdg.backend.domain.order.Order;
import be.kdg.backend.domain.order.OrderId;
import be.kdg.backend.domain.order.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoRejectSchedulerTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderService orderService;

    private AutoRejectScheduler sut;

    @BeforeEach
    void setUp() {
        sut = new AutoRejectScheduler(orderRepository, orderService);
        ReflectionTestUtils.setField(sut, "timeoutMinutes", 5);
    }

    private Order staleOrder() {
        Order order = mock(Order.class);
        when(order.id()).thenReturn(OrderId.generate());
        return order;
    }

    @Test
    void rejectsStalePlacedOrderWithAutoReason() {
        Order stale = staleOrder();
        given(orderRepository.findPlacedBefore(any())).willReturn(List.of(stale));

        sut.rejectStalePlacedOrders();

        ArgumentCaptor<String> reason = ArgumentCaptor.forClass(String.class);
        verify(orderService).cancelOrder(eq(stale.id().value()), reason.capture());
        assertThat(reason.getValue()).contains("Automatically rejected");
    }

    @Test
    void doesNothingWhenNoStaleOrders() {
        given(orderRepository.findPlacedBefore(any())).willReturn(List.of());

        sut.rejectStalePlacedOrders();

        verify(orderService, never()).cancelOrder(any(), any());
    }

    @Test
    void continuesAfterOneCancellationFails() {
        Order first = staleOrder();
        Order second = staleOrder();
        UUID firstId = first.id().value();
        UUID secondId = second.id().value();
        given(orderRepository.findPlacedBefore(any())).willReturn(List.of(first, second));
        doThrow(new IllegalStateException("boom"))
                .when(orderService).cancelOrder(eq(firstId), any());

        sut.rejectStalePlacedOrders();

        verify(orderService).cancelOrder(eq(firstId), any());
        verify(orderService).cancelOrder(eq(secondId), any());
    }

    @Test
    void computesCutoffFromConfiguredTimeout() {
        given(orderRepository.findPlacedBefore(any())).willReturn(List.of());

        sut.rejectStalePlacedOrders();

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(orderRepository).findPlacedBefore(cutoff.capture());
        LocalDateTime expected = LocalDateTime.now().minusMinutes(5);
        assertThat(cutoff.getValue()).isBetween(expected.minusSeconds(10), expected.plusSeconds(10));
    }
}
