package be.kdg.backend.application.tracking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Write/read model for US21 tracking screen.
 * Records each consumed AMQP event as a row in {@code order_event_history}.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TrackingService {

    private final OrderEventHistoryRepository historyRepository;

    public void recordEvent(UUID orderId, String eventType, String payloadJson) {
        log.debug("recordEvent orderId={} type={}", orderId, eventType);
        OrderEventEntry entry = new OrderEventEntry(null, orderId, eventType, LocalDateTime.now(), payloadJson);
        historyRepository.save(entry);
    }

    public List<OrderEventEntry> eventsFor(UUID orderId) {
        return historyRepository.findByOrderId(orderId);
    }
}