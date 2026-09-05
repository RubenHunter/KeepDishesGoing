package be.kdg.backend.infrastructure.http;

import be.kdg.backend.application.RestaurantProperties;
import be.kdg.backend.application.restaurant.RestaurantGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
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
public class RestRestaurantGateway implements RestaurantGateway {

    private final RestClient restaurantClient;
    private final RestaurantProperties restaurantProperties;
    private final double priceTolerance;

    public RestRestaurantGateway(RestClient restaurantClient,
                                 RestaurantProperties restaurantProperties,
                                 @Value("${kdg.order.price-tolerance:0.01}") double priceTolerance) {
        this.restaurantClient = restaurantClient;
        this.restaurantProperties = restaurantProperties;
        this.priceTolerance = priceTolerance;
    }

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
                                it.menuItemId(), false, live.price().amount().doubleValue(), "dish not published");
                    }
                    double tolerance = priceTolerance;
                    double livePrice = live.price().amount().doubleValue();
                    if (Math.abs(livePrice - it.unitPriceExpected()) > tolerance) {
                        return new MenuValidationResult.ItemValidation(
                                it.menuItemId(), true, livePrice, "price mismatch (expected="
                                        + it.unitPriceExpected() + ", live=" + livePrice + ")");
                    }
                    return new MenuValidationResult.ItemValidation(it.menuItemId(), true, livePrice, "ok");
                })
                .toList();

        boolean valid = results.stream().allMatch(r -> r.available() && r.message().equals("ok"));
        String message = valid ? "OK" : "One or more items failed validation";
        return new MenuValidationResult(valid, message, results);
    }

    @Override
    public RestaurantStatusDto getStatus(UUID restaurantId) {
        UpstreamStatusDto status = restaurantClient.get()
                .uri(restaurantProperties.apiBase() + "/{id}/status", restaurantId)
                .retrieve()
                .body(UpstreamStatusDto.class);
        if (status == null) throw new IllegalStateException("Empty status response for " + restaurantId);
        return new RestaurantStatusDto(restaurantId, status.openNow(), status.closingTime(), status.nextOpening());
    }

    /** Wire format of restaurant-service's RestaurantStatusResponse (additive fields only). */
    private record UpstreamStatusDto(boolean openNow, LocalDateTime closingTime, LocalDateTime nextOpening) {}

    @Override
    public RestaurantDto getRestaurant(UUID restaurantId) {
        // restaurant-service returns status (ACTIVE/INACTIVE) — map it to the open flag here.
        UpstreamRestaurantDto upstream = restaurantClient.get()
                .uri(restaurantProperties.apiBase() + "/{id}", restaurantId)
                .retrieve()
                .body(UpstreamRestaurantDto.class);
        if (upstream == null) throw new IllegalStateException("Empty restaurant response for " + restaurantId);
        return new RestaurantDto(
                upstream.id(),
                upstream.name(),
                upstream.fullAddress(),
                upstream.email(),
                upstream.openingHours(),
                upstream.logoUrl(),
                upstream.restaurantType(),
                "ACTIVE".equalsIgnoreCase(upstream.status())
        );
    }

    /** Wire format of restaurant-service's public RestaurantDto. */
    private record UpstreamRestaurantDto(
            UUID id,
            String name,
            String status,
            String fullAddress,
            String email,
            String openingHours,
            String logoUrl,
            String restaurantType
    ) {}

    @Override
    public List<DishDto> getMenu(UUID restaurantId) {
        DishDto[] dishes = restaurantClient.get()
                .uri(restaurantProperties.apiBase() + "/{id}/menu", restaurantId)
                .retrieve()
                .body(DishDto[].class);
        return dishes == null ? List.of() : Arrays.asList(dishes);
    }
}