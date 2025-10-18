package be.kdg.sa.backend.application;

import be.kdg.sa.backend.api.dto.MenuItemValidationRequest;
import be.kdg.sa.backend.api.dto.MenuItemValidationResponse;
import be.kdg.sa.backend.api.dto.RestaurantStatusResponse;
import be.kdg.sa.backend.domain.Order.*;
import be.kdg.sa.backend.domain.Shared.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderValidationService {

    private final RestTemplate restTemplate;
    private static final String RESTAURANT_SERVICE_BASE_URL = "http://localhost:8080/api";

    public ValidationResult validateOrderBeforeCheckout(Order order) {
        log.info("Validating order {} before checkout", order.getId().getValue());

        // Valideer restaurant status
        RestaurantValidationResult restaurantValidation = validateRestaurant(order.getRestaurantId());
        if (!restaurantValidation.isValid()) {
            return ValidationResult.invalid(restaurantValidation.message());
        }

        // Valideer individuele gerechten
        for (OrderItem item : order.getItems()) {
            MenuItemValidationResult itemValidation = validateMenuItem(
                    order.getRestaurantId(),
                    item.getMenuItemId(),
                    item.getUnitPrice()
            );

            if (!itemValidation.isValid()) {
                return ValidationResult.invalid(itemValidation.message());
            }
        }

        return ValidationResult.valid();
    }

    private RestaurantValidationResult validateRestaurant(RestaurantId restaurantId) {
        try {
            String url = RESTAURANT_SERVICE_BASE_URL + "/restaurants/{restaurantId}/status";
            RestaurantStatusResponse response = restTemplate.getForObject(
                    url,
                    RestaurantStatusResponse.class,
                    restaurantId.getValue()
            );

            if (response == null || !response.isOpen()) {
                return RestaurantValidationResult.invalid("Restaurant is currently closed");
            }

            return RestaurantValidationResult.valid();

        } catch (Exception e) {
            log.error("Error validating restaurant {}: {}", restaurantId.getValue(), e.getMessage());
            return RestaurantValidationResult.invalid("Unable to validate restaurant status");
        }
    }

    private MenuItemValidationResult validateMenuItem(RestaurantId restaurantId, MenuItemId menuItemId, Money expectedPrice) {
        try {
            String url = RESTAURANT_SERVICE_BASE_URL + "/restaurants/{restaurantId}/menu/{menuItemId}/validate";
            MenuItemValidationRequest request = new MenuItemValidationRequest(
                    expectedPrice.getAmount().doubleValue(),
                    expectedPrice.getCurrency()
            );

            MenuItemValidationResponse response = restTemplate.postForObject(
                    url,
                    request,
                    MenuItemValidationResponse.class,
                    restaurantId.getValue(),
                    menuItemId.getValue()
            );

            if (response == null || !response.isValid()) {
                return MenuItemValidationResult.invalid(
                        response != null ? response.message() : "Menu item is not available"
                );
            }

            return MenuItemValidationResult.valid();

        } catch (Exception e) {
            log.error("Error validating menu item {}: {}", menuItemId.getValue(), e.getMessage());
            return MenuItemValidationResult.invalid("Unable to validate menu item");
        }
    }

    // Validation Result Classes
    public record ValidationResult(boolean isValid, String message) {
        public static ValidationResult valid() {
            return new ValidationResult(true, "Validation successful");
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }
    }

    public record RestaurantValidationResult(boolean isValid, String message) {
        public static RestaurantValidationResult valid() {
            return new RestaurantValidationResult(true, null);
        }

        public static RestaurantValidationResult invalid(String message) {
            return new RestaurantValidationResult(false, message);
        }
    }

    public record MenuItemValidationResult(boolean isValid, String message) {
        public static MenuItemValidationResult valid() {
            return new MenuItemValidationResult(true, null);
        }

        public static MenuItemValidationResult invalid(String message) {
            return new MenuItemValidationResult(false, message);
        }
    }
}