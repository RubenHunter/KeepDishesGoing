package be.kdg.backend.application;

import be.kdg.backend.api.dto.DishDto;
import be.kdg.backend.api.dto.UpdateDishDto;
import be.kdg.backend.domain.Price;
import be.kdg.backend.domain.dish.*;
import be.kdg.backend.domain.restaurant.IRestaurantRepository;
import be.kdg.backend.domain.restaurant.Restaurant;
import be.kdg.backend.domain.restaurant.RestaurantId;
import be.kdg.backend.domain.scheduling.IScheduledPublishRepository;
import be.kdg.backend.domain.scheduling.ScheduledPublishJob;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

import java.util.*;

@Service
@Transactional
public class DishService {

    private final IRestaurantRepository restaurantRepository;
    private final IScheduledPublishRepository scheduledRepo;

    public DishService(IRestaurantRepository restaurantRepository,
                       IScheduledPublishRepository scheduledRepo) {
        this.restaurantRepository = restaurantRepository;
        this.scheduledRepo = scheduledRepo;
    }

    // Create a draft dish inside the Restaurant aggregate
    public DishId createDraftDish(RestaurantId restaurantId,
                                  DishName name,
                                  Description description,
                                  Price price,
                                  DishCategory category) {
        Restaurant restaurant = restaurantRepository.getById(restaurantId)
                .orElseThrow(restaurantId::notFound);

        DishId id = restaurant.createDraftDish(name, description, category, price);
        restaurantRepository.save(restaurant);
        return id;
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

    // Returns the updated dish, which may be a new draft if the original was published
    public DishDto updateDraftDish(RestaurantId restaurantId, DishId dishId, UpdateDishDto dto) {
        Restaurant restaurant = restaurantRepository.getById(restaurantId)
                .orElseThrow(restaurantId::notFound);

        DishId resultId = restaurant.updateDraftDish(
                dishId,
                dto.name(),
                dto.description(),
                dto.price(),
                dto.category()
        );

        restaurantRepository.save(restaurant);

        Dish updated = restaurant.getDishById(resultId);
        return DishDto.from(updated);
    }

    // Publish a dish through the aggregate behavior
    public void publishDish(RestaurantId restaurantId, DishId dishId) {
        Restaurant restaurant = restaurantRepository.getById(restaurantId)
                .orElseThrow(restaurantId::notFound);

        restaurant.publishDish(dishId);
        restaurantRepository.save(restaurant);
    }

    public void dePublishDish(RestaurantId restaurantId, DishId dishId) {
        Restaurant restaurant = restaurantRepository.getById(restaurantId)
                .orElseThrow(restaurantId::notFound);
        restaurant.dePublishDish(dishId);
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishAllDraftDishes(RestaurantId restaurantId) {
        Restaurant restaurant = restaurantRepository.getById(restaurantId)
                .orElseThrow(restaurantId::notFound);

        // Move loop into the aggregate
        restaurant.publishAllDraftDishes();

        restaurantRepository.save(restaurant);
    }

    public void schedulePublishAllDraftDishes(RestaurantId restaurantId, LocalDateTime publishAt) {
        restaurantRepository.getById(restaurantId).orElseThrow(restaurantId::notFound);

        if (publishAt == null) throw new IllegalArgumentException("publishAt must not be null");
        if (publishAt.isBefore(LocalDateTime.now())) throw new IllegalArgumentException("publishAt must be in the future");

        ScheduledPublishJob job = ScheduledPublishJob.create(UUID.randomUUID(), restaurantId.id(), publishAt);
        scheduledRepo.save(job);
    }

}
