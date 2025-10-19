package be.kdg.backend.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

import java.util.Objects;
import java.util.UUID;

@ValueObject
public record DeliveryPersonId(String value) {
    public DeliveryPersonId {
        validate(value);
    }

    private void validate(String value) {
        Assert.notNull(value, "DeliveryPerson ID cannot be null");
        Assert.hasText(value, "DeliveryPerson ID cannot be empty");
        if (!value.startsWith("DP-")) {
            throw new IllegalArgumentException("DeliveryPerson ID must start with 'DP-'");
        }
    }

    public static DeliveryPersonId generate() {
        return new DeliveryPersonId("DP-" + UUID.randomUUID().toString());
    }

    public static DeliveryPersonId of(String value) {
        return new DeliveryPersonId(value);
    }
}