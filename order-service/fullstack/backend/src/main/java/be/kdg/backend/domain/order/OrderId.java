package be.kdg.backend.domain.order;

import java.util.UUID;

/**
 * Strongly-typed ID for the {@link Order} aggregate root. Domain-only.
 */
public record OrderId(UUID value) {
    public OrderId {
        java.util.Objects.requireNonNull(value, "OrderId must not be null");
    }
    public static OrderId generate() { return new OrderId(UUID.randomUUID()); }
    public static OrderId of(String literal) { return new OrderId(UUID.fromString(literal)); }
}