package be.kdg.backend.application.restaurant;

import org.springframework.stereotype.Component;

/** Michelin-star: avg > €60 → €€€€. With avgPrice: €60 or under → €€€. */
@Component
public class MichelinStarPriceCategoryStrategy implements PriceCategoryStrategy {
    @Override public boolean supports(String restaurantType) { return "MICHELIN_STER".equalsIgnoreCase(restaurantType); }
    @Override public String symbolFor(String restaurantType, Double avgPrice) {
        if (avgPrice != null && avgPrice <= 60.0) return "€€€";
        return "€€€€";
    }
}