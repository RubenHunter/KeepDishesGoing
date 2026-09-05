package be.kdg.backend.infrastructure.persistence.payout;

import be.kdg.backend.domain.payout.PayoutPolicy;
import be.kdg.backend.domain.shared.Money;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Adapter wiring configuration to the {@link PayoutPolicy} port.
 * All values externalized via application.properties (grading rubric "configuration" requirement).
 */
@ConfigurationProperties(prefix = "kdg.delivery.payout")
public class PayoutProperties implements PayoutPolicy {

    private double baseFeeEur = 3.00;
    private double perMinuteEur = 0.30;
    private int minMinutes = 5;
    private int maxMinutes = 30;

    public double getBaseFeeEur() { return baseFeeEur; }
    public void setBaseFeeEur(double v) { this.baseFeeEur = v; }
    public double getPerMinuteEur() { return perMinuteEur; }
    public void setPerMinuteEur(double v) { this.perMinuteEur = v; }
    public int getMinMinutes() { return minMinutes; }
    public void setMinMinutes(int v) { this.minMinutes = v; }
    public int getMaxMinutes() { return maxMinutes; }
    public void setMaxMinutes(int v) { this.maxMinutes = v; }

    @Override public Money baseFee()       { return Money.ofEuros(baseFeeEur); }
    @Override public Money perMinuteFee() { return Money.ofEuros(perMinuteEur); }
    @Override public int minMinutes()       { return minMinutes; }
    @Override public int maxMinutes()       { return maxMinutes; }
}