package be.kdg.backend.domain.payout;

import be.kdg.backend.domain.shared.Money;

/**
 * Port (interface) owned by domain — payout policy.
 * Implementations live in infrastructure (config-driven) or test doubles (fixed-table).
 *
 * US36 rule set:
 *   billableMinutes = ceil((delivered - ready) / 60s) clamped [minMinutes, maxMinutes]
 *   total = baseFee + perMinute * billableMinutes
 */
public interface PayoutPolicy {
    Money baseFee();
    Money perMinuteFee();
    int minMinutes();
    int maxMinutes();

    /**
     * Pure computation of billable minutes based on timestamps only.
     * Caller passes ISO timestamps (no dependency on java.time inside interface signature flexibility).
     */
    default int billableMinutes(long readyEpochSeconds, long deliveredEpochSeconds) {
        long seconds = Math.max(0L, deliveredEpochSeconds - readyEpochSeconds);
        int minutes = (int) Math.ceil(seconds / 60.0);
        if (minutes < minMinutes()) return minMinutes();
        if (minutes > maxMinutes()) return maxMinutes();
        return minutes;
    }

    /**
     * Pure computation returning the total payout given an already-clamped billable-minutes count.
     */
    default Money totalFor(int billableMinutes) {
        return baseFee().add(perMinuteFee().multiply(billableMinutes));
    }
}