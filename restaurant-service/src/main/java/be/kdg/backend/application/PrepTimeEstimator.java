package be.kdg.backend.application;

import java.time.Duration;

/**
 * Estimates how long it takes to prepare an order (US14). Strategy — new estimation approaches
 * only require a new implementation, no change to the acceptance flow (coding-mistakes #8).
 */
public interface PrepTimeEstimator {
    Duration estimate();
}
