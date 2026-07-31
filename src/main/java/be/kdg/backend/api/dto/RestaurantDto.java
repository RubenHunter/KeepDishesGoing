package be.kdg.backend.api.dto;

import be.kdg.backend.domain.restaurant.Restaurant;
import be.kdg.backend.domain.restaurant.RestaurantId;
import be.kdg.backend.domain.restaurant.RestaurantName;
import be.kdg.backend.domain.restaurant.RestaurantStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.UUID;

/** Public read model — full restaurant detail (US3/US39 fields included). */
public record RestaurantDto(
        UUID id,
        @NotBlank(message = "A Restaurant needs a name")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,
        String status,
        String fullAddress,
        String email,
        String openingHours,
        String logoUrl,
        String restaurantType
) {
    public static RestaurantDto from(Restaurant restaurant) {
        return new RestaurantDto(
                restaurant.getId().id(),
                restaurant.getName().name(),
                restaurant.getStatus().name(),
                restaurant.getFullAddress(),
                restaurant.getEmail(),
                restaurant.getOpeningHours(),
                restaurant.getLogoUrl(),
                restaurant.getRestaurantType() == null ? null : restaurant.getRestaurantType().name()
        );
    }
    public Restaurant to () {
        return new Restaurant(
                //not sure about this yet, needs future testing
                RestaurantId.create(),
                new RestaurantName(this.name),
                RestaurantStatus.ACTIVE,
                new ArrayList<>()
        );
    }
}
