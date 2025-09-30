package be.kdg.sa.backend.domain.ValueObjects;

import lombok.Value;
import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
@Value
public class Quantity {
    int value;

    private Quantity(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (value > 100) {
            throw new IllegalArgumentException("Quantity cannot exceed 100");
        }
        this.value = value;
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    public Quantity add(Quantity other) {
        return new Quantity(this.value + other.value);
    }

    public Quantity subtract(Quantity other) {
        return new Quantity(this.value - other.value);
    }

    public boolean isGreaterThan(Quantity other) {
        return this.value > other.value;
    }

    public boolean isZero() {
        return this.value == 0;
    }

    public int getValue() {
        return value;
    }
}