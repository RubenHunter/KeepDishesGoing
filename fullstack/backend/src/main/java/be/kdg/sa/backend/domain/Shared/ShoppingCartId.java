package be.kdg.sa.backend.domain.Shared;

import lombok.Value;
import org.jmolecules.ddd.annotation.ValueObject;

import java.util.UUID;

@ValueObject
@Value
public class ShoppingCartId {
    String value;

    private ShoppingCartId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ShoppingCart ID cannot be null or empty");
        }
        this.value = value;
    }

    public static ShoppingCartId generate() {
        return new ShoppingCartId("CART-" + UUID.randomUUID().toString());
    }

    public static ShoppingCartId of(String value) {
        return new ShoppingCartId(value);
    }
}