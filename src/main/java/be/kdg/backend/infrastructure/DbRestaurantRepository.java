package be.kdg.backend.infrastructure;

import be.kdg.backend.domain.restaurant.*;
import be.kdg.backend.infrastructure.jpa.*;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Primary
@Repository
public class DbRestaurantRepository implements IRestaurantRepository {
    private final JpaRestaurantRepository jpaRestaurantRepository;

    public DbRestaurantRepository(JpaRestaurantRepository jpaRestaurantRepository) {
        this.jpaRestaurantRepository = jpaRestaurantRepository;
    }

    @Override
    public Restaurant insert(Restaurant restaurant) {
        JpaRestaurantEntity entity = JpaRestaurantEntity.fromDomain(restaurant);
        JpaRestaurantEntity saved = jpaRestaurantRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Restaurant> getById(RestaurantId id) {
        return jpaRestaurantRepository.findById(id.id())
                .map(JpaRestaurantEntity::toDomain);
    }

    @Override
    public Collection<Restaurant> getAll() {
        return jpaRestaurantRepository.findAll()
                .stream()
                .map(JpaRestaurantEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void update(Restaurant restaurant) {
        JpaRestaurantEntity entity = JpaRestaurantEntity.fromDomain(restaurant);
        jpaRestaurantRepository.save(entity);
    }
}
