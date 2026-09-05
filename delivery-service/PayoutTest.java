package be.kdg.backend.domain.payout;

import be.kdg.backend.domain.shared.DeliveryId;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import be.kdg.backend.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * US36 / US37 payout table — exact numbers from PDF.
 */
class PayoutTest {

    /** Configurable policy float-test reference. */
    private static final class FixedPolicy implements PayoutPolicy {
        @Override public Money baseFee()       { return Money.ofEuros(3.00); }
        @Override public Money perMinuteFee() { return Money.ofEuros(0.30); }
        @Override public int minMinutes()       { return 5; }
        @Override public int maxMinutes()       { return 30; }
    }

    private final PayoutPolicy policy = new FixedPolicy();

    @Test
    void pdfExample3min10secBills5Minutes() {
        // ready = 12:00:00 delivered = 12:03:10 → 190 s → ceil = 4 minutes but clamps to min 5
        LocalDateTime ready = LocalDateTime.of(2026,7,21,12,0,0);
        LocalDateTime delivered = ready.plusSeconds(190);
        Payout p = Payout.compute(DeliveryId.generate(), DeliveryPersonId.generate(), ready, delivered, policy);
        assertEquals(5, p.billableMinutes());
        assertEquals(0, p.total().amount().compareTo(java.math.BigDecimal.valueOf(4.50)));
    }

    @Test
    void pdfExample17min50secBills18Minutes() {
        LocalDateTime ready = LocalDateTime.of(2026,7,21,12,0,0);
        LocalDateTime delivered = ready.plusSeconds(17*60 + 50);
        Payout p = Payout.compute(DeliveryId.generate(), DeliveryPersonId.generate(), ready, delivered, policy);
        assertEquals(18, p.billableMinutes());
        assertEquals(0, p.total().amount().compareTo(java.math.BigDecimal.valueOf(8.40)));
    }

    @Test
    void pdfExample33min05secBillsMax30Minutes() {
        LocalDateTime ready = LocalDateTime.of(2026,7,21,12,0,0);
        LocalDateTime delivered = ready.plusSeconds(33*60 + 5);
        Payout p = Payout.compute(DeliveryId.generate(), DeliveryPersonId.generate(), ready, delivered, policy);
        assertEquals(30, p.billableMinutes());
        assertEquals(0, p.total().amount().compareTo(java.math.BigDecimal.valueOf(12.00)));
    }

    @Test
    void deliveredBeforeReadyThrows() {
        LocalDateTime ready = LocalDateTime.of(2026,7,21,12,0,5);
        LocalDateTime delivered = LocalDateTime.of(2026,7,21,12,0,0);
        assertThrows(IllegalArgumentException.class,
                () -> Payout.compute(DeliveryId.generate(), DeliveryPersonId.generate(), ready, delivered, policy));
    }

    @Test
    void rehydrateKeepsAllFields() {
        LocalDateTime ready = LocalDateTime.now();
        LocalDateTime delivered = ready.plusMinutes(10);
        var id = be.kdg.backend.domain.shared.PayoutId.generate();
        Payout original = Payout.compute(DeliveryId.generate(), DeliveryPersonId.generate(), ready, delivered, policy);
        Payout r = Payout.rehydrate(
                id, original.deliveryId(), original.driverId(), original.billableMinutes(),
                original.baseFee(), original.perMinuteFee(), original.total(),
                original.readyAt(), original.deliveredAt(), original.computedAt());
        assertEquals(id, r.id());
        assertEquals(original.total(), r.total());
    }
}