package be.kdg.backend.infrastructure;

import be.kdg.backend.domain.restaurant.*;
import be.kdg.backend.domain.dish.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class InMemoryRestaurantRepository implements IRestaurantRepository {
    private final Map<RestaurantId, Restaurant> data = new HashMap<>();

    public InMemoryRestaurantRepository() {
        RestaurantId id1 = RestaurantId.create();
        RestaurantId id2 = RestaurantId.create();
        RestaurantId id3 = RestaurantId.create();
        RestaurantId id4 = RestaurantId.create();
        RestaurantId id5 = RestaurantId.create();

        data.put(id1, new Restaurant(
                id1,
                new RestaurantName("Pizza Palace"),
                RestaurantStatus.ACTIVE,
                new ArrayList<>()
        ));
        data.put(id2, new Restaurant(
                id2,
                new RestaurantName("Sushi World"),
                RestaurantStatus.INACTIVE,
                new ArrayList<>()
        ));
        data.put(id3, new Restaurant(
                id3,
                new RestaurantName("Burger Barn"),
                RestaurantStatus.ACTIVE,
                new ArrayList<>()
        ));
        data.put(id4, new Restaurant(
                id4,
                new RestaurantName("Taco Town"),
                RestaurantStatus.ACTIVE,
                new ArrayList<>()
        ));
        data.put(id5, new Restaurant(
                id5,
                new RestaurantName("Curry Corner"),
                RestaurantStatus.INACTIVE,
                new ArrayList<>()
        ));
    }


    @Override
    public Restaurant insert(Restaurant restaurant) {
        data.put(restaurant.getId(), restaurant);
        return restaurant;
    }

    @Override
    public Optional<Restaurant> getById(RestaurantId id) {
        return Optional.ofNullable(data.get(id));
    }

    @Override
    public Collection<Restaurant> getAll() {
        return data.values();
    }

    @Override
    public void update(Restaurant restaurant) {
        data.put(restaurant.getId(), restaurant);
    }
}
