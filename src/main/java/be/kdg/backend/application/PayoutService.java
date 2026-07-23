package be.kdg.backend.application;

import be.kdg.backend.application.messaging.InternalEvents;
import be.kdg.backend.domain.payout.Payout;
import be.kdg.backend.domain.payout.PayoutPolicy;
import be.kdg.backend.domain.payout.PayoutRepository;
import be.kdg.backend.domain.shared.DeliveryId;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import be.kdg.backend.domain.shared.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Payout application service. US34/US35/US36/US37/US38.
 *
 * Listens to {@link InternalEvents.DeliveryDeliveredEvent} (after commit) — computes and saves a
 * {@link Payout} aggregate. US37: only on delivered (skipped on cancel — see DeliveryService.cancelClaim).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutService {

    private final PayoutRepository payoutRepository;
    private final PayoutPolicy policy;

    /** Fires AFTER the Delivery AR transaction commits — keeps tx boundaries per aggregate. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void onDeliveryDelivered(InternalEvents.DeliveryDeliveredEvent event) {
        log.info("Computing payout for delivery {} driver {}", event.deliveryId(), event.driverId());
        if (event.readyAt() == null || event.deliveredAt() == null) {
            log.error("Cannot compute payout — missing ready/delivered timestamp for delivery {}", event.deliveryId());
            return;
        }
        Payout p = Payout.compute(event.deliveryId(), event.driverId(), event.readyAt(), event.deliveredAt(), policy);
        payoutRepository.save(p);
        log.info("Payout {} saved: {} euro for {} minutes", p.id(), p.total().amount(), p.billableMinutes());
    }

    /**
     * US35 — driver payout history + running total.
     */
    @Transactional(readOnly = true)
    public DriverPayoutSummary summary(DeliveryPersonId driverId) {
        List<Payout> rows = payoutRepository.findByDriverId(driverId);
        Money total = rows.stream().map(Payout::total).reduce(Money.ofEuros(0), Money::add);
        return new DriverPayoutSummary(driverId, rows, total);
    }

    /**
     * US38 — admin report data for date range; PDF rendered in {@code infrastructure.report}.
     */
    @Transactional(readOnly = true)
    public List<Payout> reportData(DeliveryPersonId driverId, LocalDateTime from, LocalDateTime to) {
        return payoutRepository.findByDriverIdAndDateRange(driverId, from, to);
    }

    /**
     * US38 — admin report data across all drivers.
     */
    @Transactional(readOnly = true)
    public List<Payout> allPayouts(LocalDateTime from, LocalDateTime to) {
        return payoutRepository.findByDateRange(from, to);
    }

    public record DriverPayoutSummary(DeliveryPersonId driverId, List<Payout> rows, Money total) {}

    Optional<Payout> findByDelivery(DeliveryId id) {
        return payoutRepository.findByDeliveryId(id);
    }
}