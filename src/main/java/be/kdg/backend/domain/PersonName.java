package be.kdg.backend.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

@ValueObject
public record PersonName(String value) {
    public PersonName {
        validate(value);
    }

    private void validate(String value) {
        Assert.hasText(value, "Person name cannot be null or empty");
        if (value.length() < 2 || value.length() > 50) {
            throw new IllegalArgumentException("Person name must be between 2 and 50 characters");
        }
        if (!value.matches("^[a-zA-Z\\s]+$")) {
            throw new IllegalArgumentException("Person name can only contain letters and spaces");
        }
    }
}