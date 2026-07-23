package be.kdg.backend.application.restaurant;

import org.springframework.stereotype.Component;

/** Fine dining + fish restaurants: avg €31-€60 → €€€ */
@Component
public class FineDiningPriceCategoryStrategy implements PriceCategoryStrategy {
    @Override public boolean supports(String restaurantType) {
        return "FIJN_DINEREN".equalsIgnoreCase(restaurantType)
                || "VISRESTAURANTS".equalsIgnoreCase(restaurantType);
    }
    @Override public String symbolFor(String restaurantType) { return "€€€"; }
}