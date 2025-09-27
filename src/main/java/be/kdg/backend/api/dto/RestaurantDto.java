package be.kdg.backend.api.dto;

import be.kdg.backend.domain.dish.Dish;
import be.kdg.backend.domain.restaurant.Restaurant;
import be.kdg.backend.domain.restaurant.RestaurantId;
import be.kdg.backend.domain.restaurant.RestaurantName;
import be.kdg.backend.domain.restaurant.RestaurantStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

public record RestaurantDto(
        UUID id,
        @NotBlank(message = "A Restaurant needs a name")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,
        String status,
        @Valid
        List<DishDto> dishes
) {
    public static RestaurantDto from(Restaurant restaurant) {
        return new RestaurantDto(
                restaurant.getId().id(),
                restaurant.getName().name(),
                restaurant.getStatus().name(),
                restaurant.getDishes().stream().map(DishDto::from).toList()
        );
    }
    public Restaurant to () {
        return new Restaurant(
                //not sure about this yet, needs future testing
                RestaurantId.create(),
                new RestaurantName(this.name),
                RestaurantStatus.ACTIVE,
                new ArrayList<>() // or pass dishes if available
        );
    }
}
