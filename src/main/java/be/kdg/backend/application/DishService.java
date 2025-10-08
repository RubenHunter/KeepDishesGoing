package be.kdg.backend.application;

import be.kdg.backend.api.dto.DishDto;
import be.kdg.backend.api.dto.UpdateDishDto;
import be.kdg.backend.domain.Price;
import be.kdg.backend.domain.dish.*;
import be.kdg.backend.domain.restaurant.IRestaurantRepository;
import be.kdg.backend.domain.restaurant.Restaurant;
import be.kdg.backend.domain.restaurant.RestaurantId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DishService {

    private final IRestaurantRepository restaurantRepository;

    public DishService(IRestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }


    // Create a draft dish inside the Restaurant aggregate
    public DishId createDraftDish(RestaurantId restaurantId, DishName name, Price price) {
        Restaurant restaurant = restaurantRepository.getById(restaurantId)
                .orElseThrow(restaurantId::notFound);

        DishId newDishId = restaurant.createDraftDish(name, price);
        restaurantRepository.save(restaurant);
        return newDishId;
    }

    // Find a dish by its id (via the Restaurant aggregate)
    public Dish getDishById(DishId dishId) {
        Restaurant restaurant = restaurantRepository.findByDishId(dishId)
                .orElseThrow(dishId::notFound);
        return restaurant.getDishById(dishId);
    }

    // List all dishes of a restaurant (never bypass the aggregate root)
    public List<Dish> listDishesOfRestaurant(RestaurantId restaurantId) {
        Restaurant restaurant = restaurantRepository.getById(restaurantId)
                .orElseThrow(restaurantId::notFound);
        return restaurant.getDishes();
    }

    public List<Dish> getMenuDishes(RestaurantId id){
        Restaurant restaurant = restaurantRepository.getById(id)
                .orElseThrow(id::notFound);
        return restaurant.getPublishedMenu();
    }

    // Update a draft dish through the aggregate behavior
    public DishDto updateDraftDish(RestaurantId restaurantId, DishId dishId, UpdateDishDto dto) {
        Restaurant restaurant = restaurantRepository.getById(restaurantId)
                .orElseThrow(restaurantId::notFound);

        restaurant.updateDraftDish(
                dishId,
                dto.name(),
                dto.description(),
                dto.price(),
                dto.category()
        );

        restaurantRepository.save(restaurant);

        Dish updated = restaurant.getDishById(dishId);
        return DishDto.from(updated);
    }

    // Publish a dish through the aggregate behavior
    public void publishDish(RestaurantId restaurantId, DishId dishId) {
        Restaurant restaurant = restaurantRepository.getById(restaurantId)
                .orElseThrow(restaurantId::notFound);

        restaurant.publishDish(dishId);
        restaurantRepository.save(restaurant);
    }

    // Set availability by publishing or marking out of stock
    public void setDishAvailability(RestaurantId restaurantId, DishId dishId, boolean available) {
        Restaurant restaurant = restaurantRepository.getById(restaurantId)
                .orElseThrow(restaurantId::notFound);

        if (available) {
            restaurant.publishDish(dishId);
        } else {
            restaurant.markDishOutOfStock(dishId);
        }

        restaurantRepository.save(restaurant);
    }

    // Publish all DRAFT dishes (no try/catch; validations live in the domain)
    public void publishAllDraftDishes(RestaurantId restaurantId) {
        Restaurant restaurant = restaurantRepository.getById(restaurantId)
                .orElseThrow(restaurantId::notFound);

        List<Dish> drafts = restaurant.getDishes().stream()
                .filter(d -> d.getStatus() == DishStatus.DRAFT)
                .toList();

        for (Dish d : drafts) {
            restaurant.publishDish(d.getId());
        }

        restaurantRepository.save(restaurant);
    }

    public void schedulePublishAllDraftDishes(RestaurantId restaurantId, LocalDateTime publishAt) {
        // TODO: Implement scheduling logic (e.g., save to DB, trigger job)
        // For now, just log
        System.out.printf("Scheduled publish of all draft dishes for restaurant %s at %s%n", restaurantId, publishAt);
    }





}
