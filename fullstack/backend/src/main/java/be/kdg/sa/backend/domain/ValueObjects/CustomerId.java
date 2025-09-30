package be.kdg.sa.backend.domain.ValueObjects;

import lombok.Value;
import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;
import java.util.UUID;

@ValueObject
@Value
public class CustomerId {
    String value;

    private CustomerId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        this.value = value;
    }

    public static CustomerId of(String value) {
        return new CustomerId(value);
    }

    public static CustomerId generate() {
        return new CustomerId("CUST-" + UUID.randomUUID().toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerId that = (CustomerId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    public String getValue() {
        return value;
    }
}