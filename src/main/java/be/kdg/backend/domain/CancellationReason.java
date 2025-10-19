package be.kdg.backend.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

@ValueObject
public record CancellationReason(String value) {
    public CancellationReason {
        validate(value);
    }

    private void validate(String value) {
        Assert.hasText(value, "Cancellation reason cannot be null or empty");
        if (value.length() > 500) {
            throw new IllegalArgumentException("Cancellation reason cannot be longer than 500 characters");
        }
    }
}