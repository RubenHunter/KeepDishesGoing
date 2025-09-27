package be.kdg.backend.infrastructure;

import be.kdg.backend.domain.restaurant.IRestaurantRepository;
import be.kdg.backend.domain.restaurant.Restaurant;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public class RestaurantRepository implements IRestaurantRepository {


    @Override
    public Restaurant insert(Restaurant restaurant) {
        return null;
    }

    @Override
    public Optional<Restaurant> getById(long id) {
        //return Optional.of(data.get(id));
        return Optional.empty();
    }

    public Collection<Restaurant> getAll() {
        //return data.values();
        return null;
    }

    public void update(Restaurant restaurant) {
        //hier update code
    }



}
