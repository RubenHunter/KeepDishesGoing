package be.kdg.backend.application.restaurant;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Port (interface) for HTTP integration with restaurant-service. Implemented in infrastructure.
 * Domain never sees this — application uses it for menu validation + restaurant proxy.
 */
public interface RestaurantGateway {

    RestaurantDto getRestaurant(UUID restaurantId);
    List<DishDto> getMenu(UUID restaurantId);

    /** US11/US13 — real-time open/closed status computed by restaurant-service. */
    RestaurantStatusDto getStatus(UUID restaurantId);

    /** US17 validates selected items + prices against the restaurant-service live menu. */
    MenuValidationResult validateMenuItems(MenuValidationRequest request);

    record RestaurantDto(
            UUID id,
            String name,
            String fullAddress,
            String email,
            String openingHours,
            String logoUrl,
            String restaurantType,
            boolean open
    ) {}

    record RestaurantStatusDto(
            UUID restaurantId,
            boolean openNow,
            LocalDateTime closingTime,
            LocalDateTime nextOpening
    ) {}

    record DishDto(UUID id, String name, PriceDto price,
                   String status, String category, String description) {}

    record PriceDto(java.math.BigDecimal amount, String currency) {}

    record MenuValidationRequest(
            UUID restaurantId,
            List<ItemToValidate> items
    ) {
        public record ItemToValidate(UUID menuItemId, double unitPriceExpected) {}
    }

    record MenuValidationResult(boolean valid, String message, List<ItemValidation> items) {
        public record ItemValidation(UUID menuItemId, boolean available, double currentPrice, String message) {}
    }

    /** US39 price category symbol returned to the customer. */
    record PriceSymbol(String symbol) {
        public static PriceSymbol from(String s) { return new PriceSymbol(s); }
    }
}