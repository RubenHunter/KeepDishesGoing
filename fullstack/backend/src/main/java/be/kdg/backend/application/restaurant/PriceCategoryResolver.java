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

    public RestaurantGateway.PriceSymbol resolve(String restaurantType) {
        if (restaurantType == null || restaurantType.isBlank()) {
            return new RestaurantGateway.PriceSymbol(defaultStrategy.symbolFor(null));
        }
        return strategies.stream()
                .filter(s -> s.supports(restaurantType))
                .findFirst()
                .map(s -> new RestaurantGateway.PriceSymbol(s.symbolFor(restaurantType)))
                .orElseGet(() -> new RestaurantGateway.PriceSymbol(defaultStrategy.symbolFor(restaurantType)));
    }
}