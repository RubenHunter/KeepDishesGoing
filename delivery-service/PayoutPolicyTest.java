package be.kdg.backend.domain.payout;

import be.kdg.backend.domain.shared.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct unit tests of the {@link PayoutPolicy} default methods used by {@link Payout}.
 */
class PayoutPolicyTest {

    static final class FixedPolicy implements PayoutPolicy {
        @Override public Money baseFee()       { return Money.ofEuros(3.0); }
        @Override public Money perMinuteFee() { return Money.ofEuros(0.30); }
        @Override public int minMinutes()       { return 5; }
        @Override public int maxMinutes()       { return 30; }
    }

    private final PayoutPolicy policy = new FixedPolicy();

    @Test
    void negativeDifferenceClampsToMinimum() {
        assertEquals(5, policy.billableMinutes(200L, 100L));
    }

    @Test
    void underMinimumClamps() {
        assertEquals(5, policy.billableMinutes(0L, 60L)); // 1 minute → 5
        assertEquals(5, policy.billableMinutes(0L, 240L)); // 4 minutes → 5
    }

    @Test
    void ceilingRoundsSecondsToMinutes() {
        assertEquals(6, policy.billableMinutes(0L, 301L)); // 6 minutes (from 5m01s)
    }

    @Test
    void overMaximumClamps() {
        assertEquals(30, policy.billableMinutes(0L, 60L * 60));
        assertEquals(30, policy.billableMinutes(0L, 60L * 60 * 24));
    }

    @Test
    void totalForKnownMinutes() {
        assertEquals(0, policy.totalFor(5).amount().compareTo(java.math.BigDecimal.valueOf(4.50)));
        assertEquals(0, policy.totalFor(18).amount().compareTo(java.math.BigDecimal.valueOf(8.40)));
        assertEquals(0, policy.totalFor(30).amount().compareTo(java.math.BigDecimal.valueOf(12.00)));
    }
}