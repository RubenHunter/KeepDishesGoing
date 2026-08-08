package be.kdg.backend.application.restaurant;

import org.springframework.stereotype.Component;

/** Comfort food: avg €11-€30 → €€. With avgPrice: under €11 → €, €31+ → €€€. */
@Component
public class ComfortFoodPriceCategoryStrategy implements PriceCategoryStrategy {
    @Override public boolean supports(String restaurantType) { return "COMFORT_FOOD".equalsIgnoreCase(restaurantType); }
    @Override public String symbolFor(String restaurantType, Double avgPrice) {
        if (avgPrice == null) return "€€";
        if (avgPrice < 11.0) return "€";
        if (avgPrice > 30.0) return "€€€";
        return "€€";
    }
}