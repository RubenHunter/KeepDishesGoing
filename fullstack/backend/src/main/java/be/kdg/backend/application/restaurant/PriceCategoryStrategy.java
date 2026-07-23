package be.kdg.backend.application.restaurant;

/**
 * Strategy interface for converting restaurant type → price category symbol (€..€€€€) US39.
 * New types only require adding a new {@link PriceCategoryStrategy} implementation; no edit to
 * existing strategies — Open/Closed, satisfies coding-mistakes #8 (no hardcoded strategy switch).
 */
public interface PriceCategoryStrategy {
    /**
     * @return price symbol in {"€","€€","€€€","€€€€"} — or empty if not applicable.
     */
    String symbolFor(String restaurantType);

    /** True if this strategy handles the given restaurant type. */
    boolean supports(String restaurantType);
}