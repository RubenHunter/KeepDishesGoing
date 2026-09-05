package be.kdg.backend.domain.restaurant;

import be.kdg.backend.domain.dish.DishId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IRestaurantRepository {
    void save(Restaurant restaurant);

    Optional<Restaurant> getById(RestaurantId id);

    List<Restaurant> getAll();

    Optional<Restaurant> findByDishId(DishId dishId);

    // New: lightweight owner lookup (avoids initializing lazy relations)
    Optional<UUID> getOwnerId(RestaurantId id);

    // Lightweight address lookup (avoids loading the lazy dishes collection)
    Optional<String> getFullAddress(RestaurantId id);

    // US1
    boolean existsByOwnerId(UUID ownerId);

    Optional<Restaurant> findByOwnerId(UUID ownerId);
}
