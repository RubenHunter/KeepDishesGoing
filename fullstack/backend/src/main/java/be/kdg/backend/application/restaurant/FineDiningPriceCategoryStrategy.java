package be.kdg.backend.application.restaurant;

import org.springframework.stereotype.Component;

/** Fine dining + fish restaurants: avg €31-€60 → €€€. With avgPrice: under €31 → €€, €61+ → €€€€. */
@Component
public class FineDiningPriceCategoryStrategy implements PriceCategoryStrategy {
    @Override public boolean supports(String restaurantType) {
        return "FIJN_DINEREN".equalsIgnoreCase(restaurantType)
                || "VISRESTAURANTS".equalsIgnoreCase(restaurantType);
    }
    @Override public String symbolFor(String restaurantType, Double avgPrice) {
        if (avgPrice == null) return "€€€";
        if (avgPrice < 31.0) return "€€";
        if (avgPrice > 60.0) return "€€€€";
        return "€€€";
    }
}