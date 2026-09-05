package be.kdg.backend.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Default {@link PrepTimeEstimator} — a constant number of minutes from configuration
 * ({@code kdg.restaurant.prep-estimate-minutes}, default 30). No magic number in code.
 */
@Component
public class ConstantPrepTimeEstimator implements PrepTimeEstimator {

    private final Duration estimate;

    public ConstantPrepTimeEstimator(@Value("${kdg.restaurant.prep-estimate-minutes:30}") int minutes) {
        this.estimate = Duration.ofMinutes(minutes);
    }

    @Override
    public Duration estimate() {
        return estimate;
    }
}
