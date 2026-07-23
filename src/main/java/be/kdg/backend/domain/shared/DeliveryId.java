package be.kdg.backend.domain.shared;

import java.util.Objects;

/** Strongly-typed ID for the Delivery aggregate. */
public record DeliveryId(java.util.UUID value) {
    public DeliveryId { Objects.requireNonNull(value, "DeliveryId must not be null"); }
    public static DeliveryId generate() { return new DeliveryId(java.util.UUID.randomUUID()); }
    public static DeliveryId of(String literal) { return new DeliveryId(java.util.UUID.fromString(literal)); }
    public static DeliveryId of(java.util.UUID v) { return new DeliveryId(v); }
}