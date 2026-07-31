package be.kdg.backend.domain.restaurant;

/**
 * Restaurant type (US3/US39). Drives the price-category strategy in order-service:
 * FAST_FOOD/BROODJESZAKEN → €, COMFORT_FOOD → €€, FIJN_DINEREN/VISRESTAURANTS → €€€, MICHELIN_STER → €€€€.
 */
public enum RestaurantType {
    FAST_FOOD,
    BROODJESZAKEN,
    COMFORT_FOOD,
    FIJN_DINEREN,
    VISRESTAURANTS,
    MICHELIN_STER
}
