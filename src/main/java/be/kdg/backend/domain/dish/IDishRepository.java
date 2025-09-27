package be.kdg.backend.domain.dish;

import be.kdg.backend.domain.restaurant.RestaurantId;

import java.util.Collection;
import java.util.Optional;

public interface IDishRepository {
    Dish insert(Dish dish, RestaurantId restaurantId);

    Optional<Dish> getById(DishId id);

    Collection<Dish> getAllDishesFromRestaurant(RestaurantId id);


    //wordt ook gedaan via de aggregate root (restaurant)
    //Collection<Dish> getMenuDishesFromRestaurant(RestaurantId id);

    //update dish wordt gedaan via de aggregate root update (restaurant)
}
