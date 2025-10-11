package be.kdg.backend.application;

import be.kdg.backend.api.dto.RestaurantDto;
import be.kdg.backend.domain.dish.Dish;
import be.kdg.backend.domain.dish.DishId;
import be.kdg.backend.domain.restaurant.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RestaurantService {
    private final IRestaurantRepository restaurantRepository;

    public RestaurantService(IRestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }


    public List<Restaurant> listRestaurants() {
        return restaurantRepository.getAll();
    }

    // Service does not depend on DTOs
    public RestaurantId createRestaurant(final String name) {
        Restaurant restaurant = new Restaurant(
                RestaurantId.create(),
                new RestaurantName(name),
                RestaurantStatus.INACTIVE,
                new ArrayList<>()
        );
        //static create method domein regels in Restaurant domain
        restaurantRepository.save(restaurant);
        return restaurant.getId();
    }

    /*
    // Keep the original for other internal usages if any
    public Restaurant createRestaurant(final Restaurant restaurant) {
        restaurantRepository.save(restaurant);
        return restaurant;
    }
    */

    public Restaurant getRestaurantById(final RestaurantId id) {
        return restaurantRepository.getById(id)
                .orElseThrow(id::notFound);
    }

    public void openRestaurant(RestaurantId id) {
        Restaurant restaurant = restaurantRepository.getById(id).orElseThrow(id::notFound);
        restaurant.open();
        restaurantRepository.save(restaurant);
    }
    public void closeRestaurant(RestaurantId id) {
        Restaurant restaurant = restaurantRepository.getById(id).orElseThrow(id::notFound);
        restaurant.close();
        restaurantRepository.save(restaurant);
    }


}
