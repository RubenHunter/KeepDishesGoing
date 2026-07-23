package be.kdg.backend.application.restaurant;

import org.springframework.stereotype.Component;

/** Michelin-star: avg &gt; €60 → €€€€ */
@Component
public class MichelinStarPriceCategoryStrategy implements PriceCategoryStrategy {
    @Override public boolean supports(String restaurantType) { return "MICHELIN_STER".equalsIgnoreCase(restaurantType); }
    @Override public String symbolFor(String restaurantType) { return "€€€€"; }
}