package be.kdg.sa.backend.domain.Order;

import lombok.Value;
import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;
import java.util.UUID;

@ValueObject
@Value
public class OrderId {
    String value;

    private OrderId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        this.value = value;
    }

    public static OrderId generate() {
        return new OrderId("ORD-" + UUID.randomUUID().toString());
    }

    public static OrderId of(String value) {
        return new OrderId(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderId orderId = (OrderId) o;
        return Objects.equals(value, orderId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    public String getValue() {
        return value;
    }
}
