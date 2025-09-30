package be.kdg.backend.infrastructure;

import be.kdg.backend.domain.dish.*;
import be.kdg.backend.domain.restaurant.RestaurantId;
import be.kdg.backend.infrastructure.jpa.*;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Primary //to override InMemoryDishRepository when autowiring IDishRepository
@Repository
public class DbDishRepository implements IDishRepository {
    private final JpaDishRepository jpaDishRepository;
    private final JpaRestaurantRepository jpaRestaurantRepository;

    public DbDishRepository(JpaDishRepository jpaDishRepository, JpaRestaurantRepository jpaRestaurantRepository) {
        this.jpaDishRepository = jpaDishRepository;
        this.jpaRestaurantRepository = jpaRestaurantRepository;
    }

    @Override
    public Dish insert(Dish dish, RestaurantId restaurantId) {
        JpaRestaurantEntity restaurantEntity = jpaRestaurantRepository.findById(restaurantId.id())
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        JpaDishEntity entity = JpaDishEntity.fromDomain(dish, restaurantId.id());
        entity.setRestaurant(restaurantEntity);
        JpaDishEntity saved = jpaDishRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Dish> getById(DishId id) {
        return jpaDishRepository.findById(id.id())
                .map(JpaDishEntity::toDomain);
    }

    @Override
    public Collection<Dish> getAllDishesFromRestaurant(RestaurantId restaurantId) {
        return jpaDishRepository.findAll().stream()
                .filter(dish -> dish.getRestaurant().getId().equals(restaurantId.id()))
                .map(JpaDishEntity::toDomain)
                .collect(Collectors.toList());
    }
}
