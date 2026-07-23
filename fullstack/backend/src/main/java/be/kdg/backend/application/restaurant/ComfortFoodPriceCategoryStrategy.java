package be.kdg.backend.application.restaurant;

import org.springframework.stereotype.Component;

/** Comfort food: avg €11-€30 → €€ */
@Component
public class ComfortFoodPriceCategoryStrategy implements PriceCategoryStrategy {
    @Override public boolean supports(String restaurantType) { return "COMFORT_FOOD".equalsIgnoreCase(restaurantType); }
    @Override public String symbolFor(String restaurantType)  { return "€€"; }
}