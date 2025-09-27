package be.kdg.backend.api.dto;

import be.kdg.backend.domain.dish.Dish;
import be.kdg.backend.domain.restaurant.Restaurant;
import be.kdg.backend.domain.restaurant.RestaurantId;
import be.kdg.backend.domain.restaurant.RestaurantName;
import be.kdg.backend.domain.restaurant.RestaurantStatus;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

public record RestaurantDto(
        UUID id,
        String name,
        String status,
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

    public static record DishDto(
            UUID id,
            String name,
            String description,
            BigDecimal price,
            String category,
            String status
    ) {
        public static DishDto from(Dish dish) {
            return new DishDto(
                    dish.getId().id(),
                    dish.getName().name(),
                    dish.getDescription().description(),
                    dish.getPrice().amount(),
                    dish.getCategory().name(),
                    dish.getStatus().name()
            );
        }
    }
}
