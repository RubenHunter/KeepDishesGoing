package be.kdg.backend.infrastructure;

import be.kdg.backend.domain.Price;
import be.kdg.backend.domain.dish.Description;
import be.kdg.backend.domain.dish.Dish;
import be.kdg.backend.domain.dish.DishCategory;
import be.kdg.backend.domain.dish.DishId;
import be.kdg.backend.domain.dish.DishName;
import be.kdg.backend.domain.dish.DishStatus;
import be.kdg.backend.domain.restaurant.IRestaurantRepository;
import be.kdg.backend.domain.restaurant.Restaurant;
import be.kdg.backend.domain.restaurant.RestaurantId;
import be.kdg.backend.domain.restaurant.RestaurantName;
import be.kdg.backend.domain.restaurant.RestaurantStatus;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryRestaurantRepository implements IRestaurantRepository {
    private final Map<RestaurantId, Restaurant> data = new ConcurrentHashMap<>();

    public InMemoryRestaurantRepository() {
        // Seed restaurants
        Restaurant r1 = new Restaurant(
                RestaurantId.create(),
                new RestaurantName("Pizza Palace"),
                RestaurantStatus.ACTIVE,
                new ArrayList<>()
        );
        Restaurant r2 = new Restaurant(
                RestaurantId.create(),
                new RestaurantName("Sushi World"),
                RestaurantStatus.INACTIVE,
                new ArrayList<>()
        );
        Restaurant r3 = new Restaurant(
                RestaurantId.create(),
                new RestaurantName("Burger Barn"),
                RestaurantStatus.ACTIVE,
                new ArrayList<>()
        );
        Restaurant r4 = new Restaurant(
                RestaurantId.create(),
                new RestaurantName("Taco Town"),
                RestaurantStatus.ACTIVE,
                new ArrayList<>()
        );
        Restaurant r5 = new Restaurant(
                RestaurantId.create(),
                new RestaurantName("Curry Corner"),
                RestaurantStatus.INACTIVE,
                new ArrayList<>()
        );

        data.put(r1.getId(), r1);
        data.put(r2.getId(), r2);
        data.put(r3.getId(), r3);
        data.put(r4.getId(), r4);
        data.put(r5.getId(), r5);

        // Seed dishes
        Dish d1 = new Dish(
                DishId.create(),
                new DishName("Margherita Pizza"),
                new Description("Classic pizza with tomato and mozzarella"),
                new Price(BigDecimal.valueOf(10), "EUR"),
                DishCategory.MAIN_COURSE,
                DishStatus.PUBLISHED,
                "https://example.com/margherita.jpg"
        );
        Dish d2 = new Dish(
                DishId.create(),
                new DishName("Sushi Roll"),
                new Description("Fresh salmon roll"),
                new Price(BigDecimal.valueOf(12), "EUR"),
                DishCategory.MAIN_COURSE,
                DishStatus.DRAFT,
                null
        );
        Dish d3 = new Dish(
                DishId.create(),
                new DishName("Tiramisu"),
                new Description("Italian dessert"),
                new Price(BigDecimal.valueOf(6), "EUR"),
                DishCategory.DESSERT,
                DishStatus.OUT_OF_STOCK,
                null
        );

        r1.getDishes().add(d1);
        r1.getDishes().add(d3);
        r2.getDishes().add(d2);
    }

    @Override
    public void save(Restaurant restaurant) {
        data.put(restaurant.getId(), restaurant);
    }

    @Override
    public Optional<Restaurant> getById(RestaurantId id) {
        return Optional.ofNullable(data.get(id));
    }

    @Override
    public List<Restaurant> getAll() {
        return new ArrayList<>(data.values());
    }

    @Override
    public Optional<Restaurant> findByDishId(DishId dishId) {
        return data.values().stream()
                .filter(r -> r.getDishes().stream().anyMatch(d -> d.getId().equals(dishId)))
                .findFirst();
    }

    @Override
    public Optional<UUID> getOwnerId(RestaurantId id) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getFullAddress(RestaurantId id) {
        return Optional.ofNullable(data.get(id)).map(Restaurant::getFullAddress);
    }

    @Override
    public boolean existsByOwnerId(UUID ownerId) {
        return false;
    }

    @Override
    public Optional<Restaurant> findByOwnerId(UUID ownerId) {
        return Optional.empty();
    }


}
