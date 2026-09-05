package be.kdg.backend.application.messaging;

import be.kdg.backend.application.DeliveryPersonService;
import be.kdg.backend.domain.shared.DeliveryId;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Internal Spring ApplicationEvent listeners — these enforce one-aggregate-per-tx
 * by reacting to {@link InternalEvents} published by {@code DeliveryService} AFTER a Delivery-commit.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DriverAssignmentListener {

    private final DeliveryPersonService driverService;

    /**
     * After Delivery AR is committed (status=ASSIGNED), update DeliveryPerson AR in a fresh tx.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCourierAssigned(InternalEvents.CourierAssignedEvent e) {
        log.debug("Listener: CourierAssignedEvent driver→{} delivery→{}", e.driverId(), e.deliveryId());
        try {
            driverService.assignDriver(e.deliveryId(), e.driverId(), e.at());
        } catch (RuntimeException ex) {
            log.error("Driver assignment event failed (delivery={}, driver={}): {}",
                    e.deliveryId(), e.driverId(), ex.getMessage(), ex);
            // Caller can re-trigger via resilience policy. Logged per rubric.
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCourierReleased(InternalEvents.CourierReleasedEvent e) {
        log.debug("Listener: CourierReleasedEvent driver→{}", e.driverId());
        try {
            driverService.releaseDriver(e.driverId(), e.at());
        } catch (RuntimeException ex) {
            log.error("Driver release failed for {}: {}", e.driverId(), ex.getMessage(), ex);
        }
    }
}