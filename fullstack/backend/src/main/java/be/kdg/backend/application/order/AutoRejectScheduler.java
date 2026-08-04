package be.kdg.backend.application.order;

import be.kdg.backend.application.OrderService;
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
 *
 * Runs on a fixed-delay schedule. On each tick it finds all PLACED orders
 * whose placedAt timestamp is older than the configurable timeout, cancels
 * them with a descriptive reason, and publishes the OrderCancelled event
 * so the customer tracking read-model updates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoRejectScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Value("${kdg.order.auto-reject-timeout-minutes:5}")
    private int timeoutMinutes;

    @Scheduled(fixedDelayString = "${kdg.order.auto-reject-poll-seconds:30}000")
    public void rejectStalePlacedOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<Order> stale = orderRepository.findPlacedBefore(cutoff);
        if (stale.isEmpty()) return;

        log.info("Auto-reject: found {} PLACED orders older than {} min(s)", stale.size(), timeoutMinutes);
        for (Order order : stale) {
            try {
                String reason = "Automatically rejected — restaurant did not respond within "
                        + timeoutMinutes + "-minute window (US23/US24)";
                orderService.cancelOrder(order.id().value(), reason);
                log.info("Auto-rejected order {} placed at {}", order.id(), order.placedAt());
            } catch (Exception ex) {
                log.warn("Auto-reject failed for order {}: {}", order.id(), ex.getMessage());
            }
        }
    }
}
