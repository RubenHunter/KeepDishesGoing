package be.kdg.backend.application.tracking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrackingServiceTest {

    @Mock OrderEventHistoryRepository historyRepository;

    private TrackingService sut;

    @BeforeEach
    void setUp() {
        sut = new TrackingService(historyRepository);
    }

    @Test
    void recordEventPersistsEntryWithTypeAndPayload() {
        UUID orderId = UUID.randomUUID();
        sut.recordEvent(orderId, "ORDER_ACCEPTED", "{\"x\":1}");

        ArgumentCaptor<OrderEventEntry> captor = ArgumentCaptor.forClass(OrderEventEntry.class);
        verify(historyRepository).save(captor.capture());
        OrderEventEntry entry = captor.getValue();
        assertThat(entry.orderId()).isEqualTo(orderId);
        assertThat(entry.eventType()).isEqualTo("ORDER_ACCEPTED");
        assertThat(entry.payloadJson()).isEqualTo("{\"x\":1}");
        assertThat(entry.occurredAt()).isNotNull();
    }

    @Test
    void eventsForDelegatesToRepository() {
        UUID orderId = UUID.randomUUID();
        given(historyRepository.findByOrderId(orderId)).willReturn(List.of());

        assertThat(sut.eventsFor(orderId)).isEmpty();
    }
}
