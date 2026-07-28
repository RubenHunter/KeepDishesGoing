package be.kdg.backend.infrastructure.persistence.payout;

import be.kdg.backend.domain.payout.Payout;
import be.kdg.backend.domain.shared.DeliveryId;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import be.kdg.backend.domain.shared.Money;
import be.kdg.backend.domain.shared.PayoutId;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payouts", schema = "delivery")
public class JpaPayoutEntity {

    @Id
    private UUID id;
    private UUID deliveryId;
    private UUID driverId;
    private int billableMinutes;
    private BigDecimal baseFee;
    private BigDecimal perMinuteFee;
    private BigDecimal total;
    private String currency;
    private LocalDateTime readyAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime computedAt;

    public JpaPayoutEntity() {}

    public static JpaPayoutEntity from(Payout p) {
        JpaPayoutEntity e = new JpaPayoutEntity();
        e.id = p.id().value();
        e.deliveryId = p.deliveryId().value();
        e.driverId = p.driverId().value();
        e.billableMinutes = p.billableMinutes();
        e.baseFee = p.baseFee().amount();
        e.perMinuteFee = p.perMinuteFee().amount();
        e.total = p.total().amount();
        e.currency = p.total().currency();
        e.readyAt = p.readyAt();
        e.deliveredAt = p.deliveredAt();
        e.computedAt = p.computedAt();
        return e;
    }

    public Payout toDomain() {
        Money total = new Money(this.total, this.currency);
        Money base = new Money(this.baseFee, this.currency);
        Money perMin = new Money(this.perMinuteFee, this.currency);
        return Payout.rehydrate(
                PayoutId.of(id.toString()),
                DeliveryId.of(deliveryId),
                DeliveryPersonId.of(driverId),
                billableMinutes,
                base,
                perMin,
                total,
                readyAt,
                deliveredAt,
                computedAt
        );
    }
}