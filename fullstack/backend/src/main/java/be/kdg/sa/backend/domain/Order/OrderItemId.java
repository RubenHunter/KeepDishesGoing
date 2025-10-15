package be.kdg.sa.backend.domain.Order;

import lombok.Value;
import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;
import java.util.UUID;


@ValueObject
@Value
public class OrderItemId {
    String value;

    private OrderItemId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OrderItem ID cannot be null or empty");
        }
        this.value = value;
    }

    public static OrderItemId generate() {
        return new OrderItemId("ITEM-" + UUID.randomUUID().toString());
    }

    public static OrderItemId of(String value) {
        return new OrderItemId(value); // OrderItemId i.p.v. OrderId
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItemId that = (OrderItemId) o;
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
