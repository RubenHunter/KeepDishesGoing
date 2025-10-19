package be.kdg.backend.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

import java.util.Objects;
import java.util.UUID;

@ValueObject
public record OrderId(String value) {
    public OrderId {
        validate(value);
    }

    private void validate(String value) {
        Assert.notNull(value, "Order ID cannot be null");
        Assert.hasText(value, "Order ID cannot be empty");
        if (!value.startsWith("ORD-")) {
            throw new IllegalArgumentException("Order ID must start with 'ORD-'");
        }
    }

    public static OrderId of(String value) {
        return new OrderId(value);
    }
}