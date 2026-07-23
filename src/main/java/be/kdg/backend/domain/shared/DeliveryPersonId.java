package be.kdg.backend.domain.shared;

import java.util.Objects;

/** Strongly-typed ID for the DeliveryPerson (driver) aggregate. */
public record DeliveryPersonId(java.util.UUID value) {
    public DeliveryPersonId { Objects.requireNonNull(value, "DeliveryPersonId must not be null"); }
    public static DeliveryPersonId generate() { return new DeliveryPersonId(java.util.UUID.randomUUID()); }
    public static DeliveryPersonId of(String literal) { return new DeliveryPersonId(java.util.UUID.fromString(literal)); }
    public static DeliveryPersonId of(java.util.UUID v) { return new DeliveryPersonId(v); }
}