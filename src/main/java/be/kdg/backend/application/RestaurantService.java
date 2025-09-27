package be.kdg.backend.application;

import be.kdg.backend.domain.restaurant.IRestaurantRepository;
import be.kdg.backend.domain.restaurant.Restaurant;
import be.kdg.backend.domain.restaurant.RestaurantId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;

@Service
@Transactional
public class RestaurantService {
    private final IRestaurantRepository restaurantRepository;

    public RestaurantService(IRestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    public Collection<Restaurant> getAllRestaurants() {
        return restaurantRepository.getAll();
    }

    public Restaurant createRestaurant(Restaurant restaurant) {
        return restaurantRepository.insert(restaurant);
    }

    public Optional<Restaurant> getRestaurantById(RestaurantId id) {
        return restaurantRepository.getById(id);
    }

    public void updateRestaurant(Restaurant restaurant) {
        restaurantRepository.update(restaurant);
    }




}
