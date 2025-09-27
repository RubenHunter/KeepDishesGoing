package be.kdg.backend.infrastructure;

import be.kdg.backend.domain.Price;
import be.kdg.backend.domain.dish.*;
import be.kdg.backend.domain.restaurant.Restaurant;
import be.kdg.backend.domain.restaurant.RestaurantId;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;

@Repository
public class InMemoryDishRepository implements IDishRepository {
    private final Map<RestaurantId, List<Dish>> dishesByRestaurant = new HashMap<>();
    private final Map<DishId, Dish> dishById = new HashMap<>();


    public InMemoryDishRepository(InMemoryRestaurantRepository restaurantRepository) {
        List<RestaurantId> restaurantIds = restaurantRepository.getAll()
                .stream()
                .map(r -> r.getId())
                .toList();

        // Assign sample dishes to first few restaurants
        if (restaurantIds.size() >= 2) {
            RestaurantId r1Id = restaurantIds.get(0);
            RestaurantId r2Id = restaurantIds.get(1);

            Dish d1 = new Dish(
                    DishId.create(),
                    new DishName("Margherita Pizza"),
                    new Description("Classic pizza with tomato and mozzarella"),
                    new Price(BigDecimal.valueOf(10), "EUR"),
                    DishCategory.MAIN_COURSE,
                    DishStatus.PUBLISHED
            );
            Dish d2 = new Dish(
                    DishId.create(),
                    new DishName("Sushi Roll"),
                    new Description("Fresh salmon roll"),
                    new Price(BigDecimal.valueOf(12), "EUR"),
                    DishCategory.MAIN_COURSE,
                    DishStatus.DRAFT
            );
            Dish d3 = new Dish(
                    DishId.create(),
                    new DishName("Tiramisu"),
                    new Description("Italian dessert"),
                    new Price(BigDecimal.valueOf(6), "EUR"),
                    DishCategory.DESSERT,
                    DishStatus.OUT_OF_STOCK
            );

            Restaurant r1 = restaurantRepository.getById(r1Id).orElseThrow();
            Restaurant r2 = restaurantRepository.getById(r2Id).orElseThrow();

            r1.getDishes().add(d1);
            r1.getDishes().add(d3);
            r2.getDishes().add(d2);

            restaurantRepository.update(r1);
            restaurantRepository.update(r2);

            dishesByRestaurant.put(r1Id, new ArrayList<>(List.of(d1, d3)));
            dishesByRestaurant.put(r2Id, new ArrayList<>(List.of(d2)));

            dishById.put(d1.getId(), d1);
            dishById.put(d2.getId(), d2);
            dishById.put(d3.getId(), d3);
        }
    }


    @Override
    public Dish insert(Dish dish, RestaurantId restaurantId) {
        dishesByRestaurant.computeIfAbsent(restaurantId, k -> new ArrayList<>()).add(dish);
        dishById.put(dish.getId(), dish);
        return dish;
    }

    @Override
    public Optional<Dish> getById(DishId id) {
        return Optional.ofNullable(dishById.get(id));
    }

    @Override
    public Collection<Dish> getAllDishesFromRestaurant(RestaurantId id) {
        return dishesByRestaurant.getOrDefault(id, List.of());
    }
}
