package be.kdg.backend.domain.dish;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record Description(String description) {
    public Description {
        validate();
    }

    public void validate() {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }
        if (description.length() > 255) {
            throw new IllegalArgumentException("Description cannot be longer than 255 characters");
        }
    }
}
