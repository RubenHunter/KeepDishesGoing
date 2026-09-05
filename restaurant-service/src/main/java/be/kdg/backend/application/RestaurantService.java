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

    // US3 + US1: domain enforces invariant; service provides policy only
    public RestaurantId createRestaurant(final String name,
                                         final String fullAddress,
                                         final String email,
                                         final String openingHours,
                                         final String logoUrl,
                                         final UUID ownerId) {
        return createRestaurant(name, fullAddress, email, openingHours, logoUrl, ownerId, null);
    }

    // US39 — type-aware create (nullable type: default price-category strategy handles it)
    public RestaurantId createRestaurant(final String name,
                                         final String fullAddress,
                                         final String email,
                                         final String openingHours,
                                         final String logoUrl,
                                         final UUID ownerId,
                                         final RestaurantType restaurantType) {
        Restaurant restaurant = Restaurant.create(
                name, fullAddress, email, openingHours, logoUrl, ownerId, restaurantType,
                restaurantRepository::existsByOwnerId
        );
        restaurantRepository.save(restaurant);
        return restaurant.getId();
    }

    public Restaurant getRestaurantById(final RestaurantId id) {
        return restaurantRepository.getById(id)
                .orElseThrow(id::notFound);
    }

    /** US1 — every owner manages exactly one restaurant; looked up by Keycloak subject. */
    public Optional<Restaurant> getRestaurantByOwner(final UUID ownerId) {
        return restaurantRepository.findByOwnerId(ownerId);
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
