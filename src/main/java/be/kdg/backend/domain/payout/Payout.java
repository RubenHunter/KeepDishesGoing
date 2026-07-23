package be.kdg.backend.domain.payout;

import be.kdg.backend.domain.shared.DeliveryId;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import be.kdg.backend.domain.shared.Money;
import be.kdg.backend.domain.shared.PayoutId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Payout aggregate root (US34/US36/US37).
 *
 * Invariant: a Payout can only exist for a delivered delivery (US37). The {@link #compute} factory
 * enforces readiness + delivered timestamps and delegates numeric calculation to {@link PayoutPolicy}.
 */
@AggregateRoot
public class Payout {
    @Identity
    private final PayoutId id;
    private final DeliveryId deliveryId;
    private final DeliveryPersonId driverId;

    private final int billableMinutes;
    private final Money baseFee;
    private final Money perMinuteFee;
    private final Money total;

    private final LocalDateTime readyAt;
    private final LocalDateTime deliveredAt;
    private final LocalDateTime computedAt;

    private Payout(PayoutId id, DeliveryId deliveryId, DeliveryPersonId driverId,
                   int billableMinutes, Money baseFee, Money perMinuteFee, Money total,
                   LocalDateTime readyAt, LocalDateTime deliveredAt, LocalDateTime computedAt) {
        this.id = id;
        this.deliveryId = deliveryId;
        this.driverId = driverId;
        this.billableMinutes = billableMinutes;
        this.baseFee = baseFee;
        this.perMinuteFee = perMinuteFee;
        this.total = total;
        this.readyAt = readyAt;
        this.deliveredAt = deliveredAt;
        this.computedAt = computedAt;
    }

    public static Payout compute(DeliveryId deliveryId, DeliveryPersonId driverId,
                                 LocalDateTime readyAt, LocalDateTime deliveredAt,
                                 PayoutPolicy policy) {
        Objects.requireNonNull(deliveryId);
        Objects.requireNonNull(driverId);
        Objects.requireNonNull(readyAt);
        Objects.requireNonNull(deliveredAt);
        Objects.requireNonNull(policy);
        if (deliveredAt.isBefore(readyAt)) {
            throw new IllegalArgumentException("deliveredAt must not be before readyAt (US37)");
        }
        long readyEpoch = readyAt.atZone(java.time.ZoneOffset.UTC).toEpochSecond();
        long deliveredEpoch = deliveredAt.atZone(java.time.ZoneOffset.UTC).toEpochSecond();
        int billable = policy.billableMinutes(readyEpoch, deliveredEpoch);
        Money total = policy.totalFor(billable);

        return new Payout(
                PayoutId.generate(),
                deliveryId,
                driverId,
                billable,
                policy.baseFee(),
                policy.perMinuteFee(),
                total,
                readyAt,
                deliveredAt,
                LocalDateTime.now()
        );
    }

    /** Restore from persistence. */
    public static Payout rehydrate(PayoutId id, DeliveryId deliveryId, DeliveryPersonId driverId,
                                   int billableMinutes, Money baseFee, Money perMinuteFee, Money total,
                                   LocalDateTime readyAt, LocalDateTime deliveredAt, LocalDateTime computedAt) {
        return new Payout(id, deliveryId, driverId, billableMinutes, baseFee, perMinuteFee, total,
                readyAt, deliveredAt, computedAt);
    }

    public PayoutId id()                    { return id; }
    public DeliveryId deliveryId()         { return deliveryId; }
    public DeliveryPersonId driverId()      { return driverId; }
    public int billableMinutes()            { return billableMinutes; }
    public Money baseFee()                  { return baseFee; }
    public Money perMinuteFee()             { return perMinuteFee; }
    public Money total()                    { return total; }
    public LocalDateTime readyAt()          { return readyAt; }
    public LocalDateTime deliveredAt()      { return deliveredAt; }
    public LocalDateTime computedAt()       { return computedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payout other)) return false;
        return id.equals(other.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
}