package be.kdg.backend.application;

/**
 * Result of validating one menu item against the live menu (US17).
 * Plain value — no framework imports; controllers map this to the response DTO.
 */
public record MenuItemValidation(
        boolean isValid,
        String message,
        Double currentPrice,
        String currentCurrency,
        boolean isAvailable
) {
    public static MenuItemValidation notFound() {
        return new MenuItemValidation(false, "Menu item not found", null, null, false);
    }
}
