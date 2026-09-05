package be.kdg.backend.domain.shoppingcart;

import java.util.UUID;

/**
 * Aggregate identity value object for {@link ShoppingCart}. Domain-only.
 */
public record CartId(UUID value) {
    public CartId {
        java.util.Objects.requireNonNull(value, "CartId must not be null");
    }
    public static CartId generate() { return new CartId(UUID.randomUUID()); }
    public static CartId of(String literal) { return new CartId(UUID.fromString(literal)); }
}