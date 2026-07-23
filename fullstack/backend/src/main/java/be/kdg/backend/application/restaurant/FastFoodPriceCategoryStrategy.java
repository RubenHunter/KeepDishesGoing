package be.kdg.backend.application.restaurant;

import org.springframework.stereotype.Component;

/**
 * Default price-category strategy per PDF table:
 *   FAST_FOOD / BROODJESZAKEN   → €  (avg &lt; €10)
 * This strategy supports the simple-subset type cases; additional strategies
 * added as separate {@link PriceCategoryStrategy} beans (Open/Closed, rule #8).
 */
@Component
public class FastFoodPriceCategoryStrategy implements PriceCategoryStrategy {

    @Override
    public boolean supports(String restaurantType) {
        return "FAST_FOOD".equalsIgnoreCase(restaurantType)
                || "BROODJESZAKEN".equalsIgnoreCase(restaurantType)
                || restaurantType == null
                || restaurantType.isBlank(); // fall-back path for missing type
    }

    @Override
    public String symbolFor(String restaurantType) {
        return "€";
    }
}