package be.kdg.backend.application;

import be.kdg.backend.api.dto.DishDto;
import be.kdg.backend.api.dto.UpdateDishDto;
import be.kdg.backend.domain.dish.*;
import be.kdg.backend.domain.restaurant.IRestaurantRepository;
import be.kdg.backend.domain.restaurant.Restaurant;
import be.kdg.backend.domain.restaurant.RestaurantId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

import java.util.Collection;
import java.util.Optional;

@Service
@Transactional
public class DishService {
    private final IDishRepository dishRepository;
    private final IRestaurantRepository restaurantRepository;

    public DishService(IDishRepository dishRepository, IRestaurantRepository restaurantRepository) {
        this.dishRepository = dishRepository;
        this.restaurantRepository = restaurantRepository;
    }

    public Dish createDish(Dish dish, RestaurantId restaurantId) {
        // Insert dish into repository
        Dish createdDish = dishRepository.insert(dish, restaurantId);

        // Also add dish to the Restaurant aggregate
        Restaurant restaurant = restaurantRepository.getById(restaurantId)
                .orElseThrow(restaurantId::notFound);
        restaurant.getDishes().add(createdDish);
        restaurantRepository.update(restaurant);

        return createdDish;
    }

    public Optional<Dish> getDishById(DishId id){
        return dishRepository.getById(id);
    }

    public Collection<Dish> getAllDishesFromRestaurantById(RestaurantId id){
        return dishRepository.getAllDishesFromRestaurant(id);
    }

    public Collection<Dish> getMenuDishes(RestaurantId id){
        Restaurant restaurant = restaurantRepository.getById(id)
                .orElseThrow(id::notFound);
        return restaurant.getPublishedMenu();
    }

    //sinds we restaurants updaten (de aggregate root) moeten we niet de dish repository updaten
    public DishDto updateDish(RestaurantId restaurantId, DishId dishId, UpdateDishDto dto) {
        Restaurant restaurant = restaurantRepository.getById(restaurantId)
                .orElseThrow(restaurantId::notFound);

        restaurant.updateDraftDish(
                dishId,
                dto.name(),
                dto.description(),
                dto.price(),
                dto.category()
        );

        restaurantRepository.update(restaurant);
        Dish dish = restaurant.getDishes().stream()
                .filter(d -> d.getId().equals(dishId))
                .findFirst()
                .orElseThrow(dishId::notFound);
        return DishDto.from(dish);
    }

    public void publishDish(RestaurantId restaurantId, DishId dishId) {
        Restaurant restaurant = restaurantRepository.getById(restaurantId)
                .orElseThrow(restaurantId::notFound);

        restaurant.publishDish(dishId);

        restaurantRepository.update(restaurant);
    }

    public void setDishAvailability(RestaurantId restaurantId, DishId dishId, boolean available) {
        Restaurant restaurant = restaurantRepository.getById(restaurantId)
                .orElseThrow(restaurantId::notFound);

        if (available) {
            restaurant.publishDish(dishId);
        } else {
            restaurant.markDishOutOfStock(dishId);
        }

        restaurantRepository.update(restaurant);
    }

    public void publishAllDraftDishes(RestaurantId restaurantId) {
        Restaurant restaurant = restaurantRepository.getById(restaurantId)
                .orElseThrow(restaurantId::notFound);

        restaurant.getDishes().stream()
                .filter(dish -> dish.getStatus() == DishStatus.DRAFT)
                .forEach(dish -> {
                    try {
                        restaurant.publishDish(dish.getId());
                    } catch (IllegalStateException ignored) {
                        // Optionally log or handle already published/out of stock
                    }
                });

        restaurantRepository.update(restaurant);
    }

    public void schedulePublishAllDraftDishes(RestaurantId restaurantId, LocalDateTime publishAt) {
        // TODO: Implement scheduling logic (e.g., save to DB, trigger job)
        // For now, just log
        System.out.printf("Scheduled publish of all draft dishes for restaurant %s at %s%n", restaurantId, publishAt);
    }





}
