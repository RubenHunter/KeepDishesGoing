package be.kdg.backend.application;

import be.kdg.backend.domain.IRestaurantRepository;
import be.kdg.backend.domain.Restaurant;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
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

    public Optional<Restaurant> getRestaurantById(long id) {
        return restaurantRepository.getById(id);
    }

    public void updateRestaurant(Restaurant restaurant) {
        restaurantRepository.update(restaurant);
    }




}
