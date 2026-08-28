package be.kdg.backend.api;

import be.kdg.backend.application.restaurant.PriceCategoryResolver;
import be.kdg.backend.application.restaurant.RestaurantGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Public proxy endpoints exposing restaurant-service to the customer-facing UI.
 * Order-service is the only public entry point per PDF ("Customer → order service public"), so it must
 * expose enough of the restaurant-service read API for browsing (US15 / US39).
 */
@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantProxyController {

    private final RestaurantGateway restaurantGateway;
    private final PriceCategoryResolver priceCategoryResolver;

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantGateway.RestaurantDto> getRestaurant(@PathVariable UUID id) {
        return ResponseEntity.ok(restaurantGateway.getRestaurant(id));
    }

    @GetMapping("/{id}/menu")
    public ResponseEntity<List<RestaurantGateway.DishDto>> getMenu(@PathVariable UUID id) {
        return ResponseEntity.ok(restaurantGateway.getMenu(id));
    }

    /** US13 — hours-aware open/closed (server-computed by restaurant-service). */
    @GetMapping("/{id}/status")
    public ResponseEntity<RestaurantGateway.RestaurantStatusDto> getStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(restaurantGateway.getStatus(id));
    }

    /** US39 — price category symbol for restaurant type + average menu price. */
    @GetMapping("/{id}/price-category")
    public ResponseEntity<PriceCategoryResponse> priceCategory(@PathVariable UUID id) {
        RestaurantGateway.RestaurantDto dto = restaurantGateway.getRestaurant(id);
        List<RestaurantGateway.DishDto> menu = restaurantGateway.getMenu(id);
        Double avgPrice = priceCategoryResolver.averagePrice(menu);
        RestaurantGateway.PriceSymbol symbol = priceCategoryResolver.resolve(dto.restaurantType(), avgPrice);
        return ResponseEntity.ok(new PriceCategoryResponse(symbol.symbol()));
    }

    public record PriceCategoryResponse(String symbol) {}
}