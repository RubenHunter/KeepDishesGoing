package be.kdg.backend.application.restaurant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * US39 strategy registry. Looks up a strategy that supports the restaurant type;
 * falls back to a default strategy. Beans of {@link PriceCategoryStrategy} are
 * auto-discovered (Spring @Component scan) and injected into the constructor.
 */
@Configuration
public class PriceCategoryResolver {

    private final PriceCategoryStrategy defaultStrategy;
    private final java.util.List<PriceCategoryStrategy> strategies;

    public PriceCategoryResolver(
            java.util.List<PriceCategoryStrategy> strategies,
            @Value("${kdg.order.price-category.default:FAST_FOOD}") String defaultType
    ) {
        this.strategies = strategies;
        this.defaultStrategy = strategies.stream()
                .filter(s -> s.supports(defaultType))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No default PriceCategoryStrategy for " + defaultType));
    }

    public RestaurantGateway.PriceSymbol resolve(String restaurantType, Double avgPrice) {
        if (restaurantType == null || restaurantType.isBlank()) {
            return new RestaurantGateway.PriceSymbol(defaultStrategy.symbolFor(null, avgPrice));
        }
        return strategies.stream()
                .filter(s -> s.supports(restaurantType))
                .findFirst()
                .map(s -> new RestaurantGateway.PriceSymbol(s.symbolFor(restaurantType, avgPrice)))
                .orElseGet(() -> new RestaurantGateway.PriceSymbol(defaultStrategy.symbolFor(restaurantType, avgPrice)));
    }

    /**
     * US39 — average price of the published menu (null when fewer than 2 priced items).
     * Kept here so the controller stays a thin mapper over the gateway + resolver.
     */
    public Double averagePrice(java.util.List<RestaurantGateway.DishDto> menu) {
        if (menu == null || menu.isEmpty()) return null;
        double total = 0;
        int count = 0;
        for (RestaurantGateway.DishDto d : menu) {
            if (d.price() != null && d.price().amount() != null) {
                total += d.price().amount().doubleValue();
                count++;
            }
        }
        return count < 2 ? null : total / count;
    }
}