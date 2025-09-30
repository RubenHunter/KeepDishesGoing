package be.kdg.backend.infrastructure.jpa;

import be.kdg.backend.domain.dish.Dish;
import be.kdg.backend.domain.restaurant.Restaurant;
import be.kdg.backend.domain.restaurant.RestaurantId;
import be.kdg.backend.domain.restaurant.RestaurantName;
import be.kdg.backend.domain.restaurant.RestaurantStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "restaurants")
public class JpaRestaurantEntity {

    @Getter
    @Id
    private UUID id;

    @Column
    private String name;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private RestaurantStatus status;

    @Getter
    @Setter
    @OneToMany(mappedBy = "restaurant", fetch = FetchType.LAZY, orphanRemoval = true)
    private List<JpaDishEntity> dishes = new ArrayList<>();

    protected JpaRestaurantEntity() {
    }

    public JpaRestaurantEntity(UUID id, String name, RestaurantStatus status) {
        this.id = id;
        this.name = name;
        this.status = status;
    }

    public static JpaRestaurantEntity fromDomain(Restaurant restaurant){
        JpaRestaurantEntity jpaRestaurantEntity = new JpaRestaurantEntity(
                restaurant.getId().id(),
                restaurant.getName().name(),
                restaurant.getStatus()
        );

        List<JpaDishEntity> jpaDishEntities = restaurant.getDishes().stream()
                .map(dish -> JpaDishEntity.fromDomain(dish, restaurant.getId().id()))
                .toList();

        jpaRestaurantEntity.dishes.addAll(jpaDishEntities);

        return jpaRestaurantEntity;
    }

    public Restaurant toDomain() {
        List<Dish> domainDishes = dishes.stream()
                .map(JpaDishEntity::toDomain)
                .toList();

        return new Restaurant(
                new RestaurantId(id),
                new RestaurantName(name),
                status,
                new ArrayList<>(domainDishes)
        );
    }


}
