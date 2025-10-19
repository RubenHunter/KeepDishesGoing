package be.kdg.backend.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

import java.util.Objects;
import java.util.UUID;

@ValueObject
public record DeliveryId(String value) {
    public DeliveryId {
        validate(value);
    }

    private void validate(String value) {
        Assert.notNull(value, "Delivery ID cannot be null");
        Assert.hasText(value, "Delivery ID cannot be empty");
        if (!value.startsWith("DEL-")) {
            throw new IllegalArgumentException("Delivery ID must start with 'DEL-'");
        }
    }

    public static DeliveryId generate() {
        return new DeliveryId("DEL-" + UUID.randomUUID().toString());
    }

    public static DeliveryId of(String value) {
        return new DeliveryId(value);
    }
}