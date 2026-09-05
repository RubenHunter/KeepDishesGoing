package be.kdg.backend.domain.shared;

import java.util.Objects;

/**
 * Strongly-typed ID for a customer. Plain UUID-backed value object — domain only.
 */
public record CustomerId(java.util.UUID value) {
    public CustomerId {
        Objects.requireNonNull(value, "CustomerId must not be null");
    }
    public static CustomerId of(java.util.UUID value) { return new CustomerId(value); }
    public static CustomerId generate() { return new CustomerId(java.util.UUID.randomUUID()); }
}