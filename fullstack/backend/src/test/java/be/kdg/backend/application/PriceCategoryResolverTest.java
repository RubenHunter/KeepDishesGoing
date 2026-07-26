package be.kdg.backend.application.restaurant;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class PriceCategoryResolverTest {

    private final List<PriceCategoryStrategy> strategies = List.of(
            new FastFoodPriceCategoryStrategy(),
            new ComfortFoodPriceCategoryStrategy(),
            new FineDiningPriceCategoryStrategy(),
            new MichelinStarPriceCategoryStrategy()
    );
    private final PriceCategoryResolver resolver = new PriceCategoryResolver(strategies, "FAST_FOOD");

    @Test
    void fastFood() {
        assertEquals("€", resolver.resolve("FAST_FOOD").symbol());
    }

    @Test
    void broodjes() {
        assertEquals("€", resolver.resolve("BROODJESZAKEN").symbol());
    }

    @Test
    void comfortFood() {
        assertEquals("€€", resolver.resolve("COMFORT_FOOD").symbol());
    }

    @Test
    void fijnDineren() {
        assertEquals("€€€", resolver.resolve("FIJN_DINEREN").symbol());
    }

    @Test
    void visrestaurants() {
        assertEquals("€€€", resolver.resolve("VISRESTAURANTS").symbol());
    }

    @Test
    void michelin() {
        assertEquals("€€€€", resolver.resolve("MICHELIN_STER").symbol());
    }

    @Test
    void unknownFallsBackToDefault() {
        assertEquals("€", resolver.resolve("???").symbol());
    }

    @Test
    void blankFallsBack() {
        assertEquals("€", resolver.resolve("").symbol());
        assertEquals("€", resolver.resolve(null).symbol());
    }
}