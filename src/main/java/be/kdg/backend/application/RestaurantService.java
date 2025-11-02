package be.kdg.backend.application;

import be.kdg.backend.domain.DomainConflictException;
import be.kdg.backend.domain.restaurant.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
        return createRestaurant(
                name,
                "Test Street 1, 1000 TestCity, BE",
                "test@example.com",
                "Mon-Sun 00:00-23:59",
                "https://example.com/logo.png",
                UUID.randomUUID()
        );
    }
    // Overload voor createRestaurant met ownerId
    public RestaurantId createRestaurant(final String name, UUID ownerId) { // new
        Restaurant restaurant = Restaurant.create(name, ownerId);
        restaurantRepository.save(restaurant);
        return restaurant.getId();
    }

    // US3 + US1: domain enforces invariant; service provides policy only
    public RestaurantId createRestaurant(final String name,
                                         final String fullAddress,
                                         final String email,
                                         final String openingHours,
                                         final String logoUrl,
                                         final UUID ownerId) {
        Restaurant restaurant = Restaurant.create(
                name, fullAddress, email, openingHours, logoUrl, ownerId,
                restaurantRepository::existsByOwnerId
        );
        restaurantRepository.save(restaurant);
        return restaurant.getId();
    }

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
