package be.kdg.backend.domain.shared;

import java.util.Objects;

/** Cross-service reference to order-service's OrderId by value. Domain-only. */
public record OrderId(java.util.UUID value) {
    public OrderId { Objects.requireNonNull(value, "OrderId must not be null"); }
    public static OrderId of(java.util.UUID v) { return new OrderId(v); }
    public static OrderId of(String literal) { return new OrderId(java.util.UUID.fromString(literal)); }
}