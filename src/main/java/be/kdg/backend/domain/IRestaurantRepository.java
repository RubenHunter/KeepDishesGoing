package be.kdg.backend.domain;

import java.util.Collection;
import java.util.Optional;

public interface IRestaurantRepository {
    Restaurant insert(Restaurant restaurant);

    Optional<Restaurant> getById(long id);

    Collection<Restaurant> getAll();

    void update(Restaurant restaurant);



}
