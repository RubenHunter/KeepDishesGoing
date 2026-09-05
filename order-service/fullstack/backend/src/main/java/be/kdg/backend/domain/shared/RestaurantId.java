package be.kdg.backend.domain.shared;

import java.util.Objects;

/**
 * Strongly-typed ID for a restaurant. Cross-service reference-by-ID — domain only.
 */
public record RestaurantId(java.util.UUID value) {
    public RestaurantId {
        Objects.requireNonNull(value, "RestaurantId must not be null");
    }
    public static RestaurantId of(java.util.UUID value) { return new RestaurantId(value); }
    public static RestaurantId of(String literal) { return new RestaurantId(java.util.UUID.fromString(literal)); }
}