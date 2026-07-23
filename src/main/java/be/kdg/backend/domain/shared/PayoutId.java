package be.kdg.backend.domain.shared;

import java.util.Objects;

/** Strongly-typed ID for the Payout aggregate. */
public record PayoutId(java.util.UUID value) {
    public PayoutId { Objects.requireNonNull(value, "PayoutId must not be null"); }
    public static PayoutId generate() { return new PayoutId(java.util.UUID.randomUUID()); }
    public static PayoutId of(String literal) { return new PayoutId(java.util.UUID.fromString(literal)); }
}