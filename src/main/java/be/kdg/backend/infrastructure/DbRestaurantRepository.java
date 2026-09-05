package be.kdg.backend.infrastructure;

import be.kdg.backend.domain.dish.DishId;
import be.kdg.backend.domain.restaurant.*;
import be.kdg.backend.infrastructure.jpa.*;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Primary
@Repository
@Transactional(readOnly = true)
public class DbRestaurantRepository implements IRestaurantRepository {
    private final JpaRestaurantRepository jpaRestaurantRepository;

    public DbRestaurantRepository(JpaRestaurantRepository jpaRestaurantRepository) {
        this.jpaRestaurantRepository = jpaRestaurantRepository;
    }

    @Override
    @Transactional
    public void save(Restaurant restaurant) {
        final UUID id = restaurant.getId().id();

        Optional<JpaRestaurantEntity> maybeManaged = jpaRestaurantRepository.findById(id);
        if (maybeManaged.isPresent()) {
            JpaRestaurantEntity managed = maybeManaged.get();
            managed.applyFromDomain(restaurant);
            return;
        }

        JpaRestaurantEntity entity = JpaRestaurantEntity.fromDomain(restaurant);
        jpaRestaurantRepository.save(entity);
    }

    @Override
    public Optional<Restaurant> getById(RestaurantId id) {
        return jpaRestaurantRepository.findById(id.id())
                .map(JpaRestaurantEntity::toDomain);
    }

    @Override
    public List<Restaurant> getAll() {
        return jpaRestaurantRepository.findAll()
                .stream()
                .map(JpaRestaurantEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Restaurant> findByDishId(DishId dishId) {
        return jpaRestaurantRepository.findByDishes_Id(dishId.id())
                .map(JpaRestaurantEntity::toDomain);
    }

    @Override
    public Optional<UUID> getOwnerId(RestaurantId id) {
        return jpaRestaurantRepository.findOwnerIdById(id.id());
    }

    @Override
    public Optional<String> getFullAddress(RestaurantId id) {
        return jpaRestaurantRepository.findFullAddressById(id.id());
    }

    @Override
    public boolean existsByOwnerId(UUID ownerId) {
        return jpaRestaurantRepository.existsByOwnerId(ownerId);
    }

    @Override
    public Optional<Restaurant> findByOwnerId(UUID ownerId) {
        return jpaRestaurantRepository.findByOwnerId(ownerId).map(JpaRestaurantEntity::toDomain);
    }
}
