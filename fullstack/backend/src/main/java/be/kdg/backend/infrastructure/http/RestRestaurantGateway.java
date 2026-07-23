package be.kdg.backend.infrastructure.http;

import be.kdg.backend.application.RestaurantProperties;
import be.kdg.backend.application.restaurant.RestaurantGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * HTTP adapter implementing the {@link RestaurantGateway} port (US17 menu validation + US39 proxy).
 * Talks to restaurant-service via plain {@link RestClient} over HTTP — no infra leaks to domain.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestRestaurantGateway implements RestaurantGateway {

    private final RestClient restaurantClient;
    private final RestaurantProperties restaurantProperties;

    /** US17: validate every selected item against the restaurant-service live menu. */
    @Override
    public MenuValidationResult validateMenuItems(MenuValidationRequest request) {
        log.debug("validateMenuItems restaurant={} items={}", request.restaurantId(), request.items().size());
        // Fetch live menu; treat 404 / network failure as "restaurant unavailable" → invalid result.
        List<DishDto> liveMenu;
        try {
            DishDto[] dishes = restaurantClient.get()
                    .uri(restaurantProperties.apiBase() + "/{id}/menu", request.restaurantId())
                    .retrieve()
                    .body(DishDto[].class);
            liveMenu = dishes == null ? List.of() : Arrays.asList(dishes);
        } catch (org.springframework.web.client.RestClientResponseException ex) {
            log.warn("restaurant-service returned {} for menu lookup (restaurantId={})",
                    ex.getStatusCode(), request.restaurantId());
            return new MenuValidationResult(false, "Restaurant menu unavailable", List.of());
        } catch (org.springframework.web.client.ResourceAccessException ex) {
            log.error("restaurant-service unreachable for menu lookup (restaurantId={}): {}",
                    request.restaurantId(), ex.getMessage());
            return new MenuValidationResult(false, "Restaurant service unreachable", List.of());
        }

        java.util.Map<UUID, DishDto> liveById = liveMenu.stream()
                .collect(Collectors.toMap(DishDto::id, d -> d));

        var results = request.items().stream()
                .map(it -> {
                    DishDto live = liveById.get(it.menuItemId());
                    if (live == null) {
                        return new MenuValidationResult.ItemValidation(
                                it.menuItemId(), false, 0.0, "menu item not found");
                    }
                    if (!"PUBLISHED".equalsIgnoreCase(live.status())) {
                        return new MenuValidationResult.ItemValidation(
                                it.menuItemId(), false, live.price(), "dish not published");
                    }
                    double tolerance = 0.01;
                    if (Math.abs(live.price() - it.unitPriceExpected()) > tolerance) {
                        return new MenuValidationResult.ItemValidation(
                                it.menuItemId(), true, live.price(), "price mismatch (expected="
                                        + it.unitPriceExpected() + ", live=" + live.price() + ")");
                    }
                    return new MenuValidationResult.ItemValidation(it.menuItemId(), true, live.price(), "ok");
                })
                .toList();

        boolean valid = results.stream().allMatch(r -> r.available() && r.message().equals("ok"));
        String message = valid ? "OK" : "One or more items failed validation";
        return new MenuValidationResult(valid, message, results);
    }

    @Override
    public RestaurantDto getRestaurant(UUID restaurantId) {
        return restaurantClient.get()
                .uri(restaurantProperties.apiBase() + "/{id}", restaurantId)
                .retrieve()
                .body(RestaurantDto.class);
    }

    @Override
    public List<DishDto> getMenu(UUID restaurantId) {
        DishDto[] dishes = restaurantClient.get()
                .uri(restaurantProperties.apiBase() + "/{id}/menu", restaurantId)
                .retrieve()
                .body(DishDto[].class);
        return dishes == null ? List.of() : Arrays.asList(dishes);
    }
}