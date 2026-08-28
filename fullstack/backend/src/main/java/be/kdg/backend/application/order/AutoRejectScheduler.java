package be.kdg.backend.application.order;

import be.kdg.backend.application.OrderService;
import be.kdg.backend.application.tracking.TrackingService;
import be.kdg.backend.domain.order.Order;
import be.kdg.backend.domain.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * US23/US24 — automatically rejects orders that remain in PLACED state
 * longer than the configured decision window (default 5 minutes).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoRejectScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final TrackingService trackingService;

    @Value("${kdg.order.auto-reject-timeout-minutes:5}")
    private int timeoutMinutes;

    @Scheduled(fixedDelayString = "PT${kdg.order.auto-reject-poll-seconds:30}S")
    public void rejectStalePlacedOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<Order> stale = orderRepository.findPlacedBefore(cutoff);
        if (stale.isEmpty()) return;

        log.info("Auto-reject: found {} PLACED orders older than {} min(s)", stale.size(), timeoutMinutes);
        for (Order order : stale) {
            try {
                String reason = "Automatically rejected — restaurant did not respond within "
                        + timeoutMinutes + "-minute window (US23/US24)";
                orderService.rejectOrder(order.id().value(), reason);
                trackingService.recordEvent(order.id().value(), "ORDER_REJECTED",
                        "{\"orderId\":\"" + order.id() + "\",\"reason\":\"" + reason + "\"}");
                log.info("Auto-rejected order {} placed at {}", order.id(), order.placedAt());
            } catch (Exception ex) {
                log.warn("Auto-reject failed for order {}: {}", order.id(), ex.getMessage());
            }
        }
    }
}
