package be.kdg.sa.backend.domain.Order;

import lombok.Value;
import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;
import java.util.UUID;

@ValueObject
@Value
public class RestaurantId {
    String value;

    private RestaurantId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Restaurant ID cannot be null or empty");
        }
        this.value = value;
    }

    public static RestaurantId of(String value) {
        return new RestaurantId(value);
    }

    public static RestaurantId generate() {
        return new RestaurantId("REST-" + UUID.randomUUID().toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RestaurantId that = (RestaurantId) o;
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