package be.kdg.backend.domain.shared;

import java.util.Objects;

/**
 * Strongly-typed ID for a menu item (= dish in restaurant-service). Cross-service reference-by-ID.
 */
public record MenuItemId(java.util.UUID value) {
    public MenuItemId {
        Objects.requireNonNull(value, "MenuItemId must not be null");
    }
    public static MenuItemId of(java.util.UUID value) { return new MenuItemId(value); }
    public static MenuItemId of(String literal) { return new MenuItemId(java.util.UUID.fromString(literal)); }
}