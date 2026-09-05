package be.kdg.backend.application.restaurant;

import org.springframework.stereotype.Component;

/** Fast food / broodjeszaken: avg <€10 → €. With avgPrice: under €10 stays €, €10+ upgrades to €€. */
@Component
public class FastFoodPriceCategoryStrategy implements PriceCategoryStrategy {
    @Override public boolean supports(String restaurantType) {
        return "FAST_FOOD".equalsIgnoreCase(restaurantType) || "BROODJESZAKEN".equalsIgnoreCase(restaurantType);
    }
    @Override public String symbolFor(String restaurantType, Double avgPrice) {
        if (avgPrice != null && avgPrice >= 10.0) return "€€";
        return "€";
    }
}