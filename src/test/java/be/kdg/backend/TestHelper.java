package be.kdg.backend;

import be.kdg.backend.application.DishService;
import be.kdg.backend.application.RestaurantService;
import be.kdg.backend.domain.Price;
import be.kdg.backend.domain.dish.*;
import be.kdg.backend.domain.restaurant.Restaurant;
import be.kdg.backend.domain.restaurant.RestaurantId;
import be.kdg.backend.infrastructure.jpa.JpaRestaurantRepository;
import be.kdg.backend.infrastructure.jpa.JpaScheduledPublishRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class TestHelper {
    @Autowired
    private RestaurantService restaurantService;
    @Autowired
    private DishService dishService;

    @Autowired
    private JpaRestaurantRepository jpaRestaurantRepository;
    @Autowired
    private JpaScheduledPublishRepository jpaScheduledPublishRepository;

    public Restaurant createRestaurant(String name) {
        RestaurantId id = restaurantService.createRestaurant(
                name,
                name + " 1, 2000 Antwerpen, België",
                "owner-" + name + "@example.com",
                "Mon-Sun 09:00-22:00",
                "https://example.com/logo.png",
                UUID.randomUUID()
        );
        return restaurantService.getRestaurantById(id);
    }

    public DishId addDraftDish(Restaurant restaurant, String name, BigDecimal amount, String currency, DishCategory category, String description) {
        return dishService.createDraftDish(
                restaurant.getId(),
                new DishName(name),
                new Description(description),
                new Price(amount, currency),
                category,
                null
        );
    }

    public void publishDish(Restaurant restaurant, DishId dishId) {
        dishService.publishDish(restaurant.getId(), dishId);
    }

    public Restaurant reload(RestaurantId id) {
        return restaurantService.getRestaurantById(id);
    }

    public void cleanUp() {
        jpaScheduledPublishRepository.deleteAll();
        jpaRestaurantRepository.deleteAll();
    }

    public RestaurantId id(Restaurant restaurant) {
        return restaurant.getId();
    }

    public DishId randomDishId() {
        return new DishId(UUID.randomUUID());
    }
}
