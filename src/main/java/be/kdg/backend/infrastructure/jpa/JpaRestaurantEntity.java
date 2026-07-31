package be.kdg.backend.infrastructure.jpa;

import be.kdg.backend.domain.dish.Dish;
import be.kdg.backend.domain.restaurant.Restaurant;
import be.kdg.backend.domain.restaurant.RestaurantId;
import be.kdg.backend.domain.restaurant.RestaurantName;
import be.kdg.backend.domain.restaurant.RestaurantStatus;
import be.kdg.backend.domain.restaurant.RestaurantType;
import jakarta.persistence.*;
import lombok.Setter;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Entity
@Table(name = "restaurants",
        uniqueConstraints = {
                // US1: one restaurant per owner
                @UniqueConstraint(name = "uk_restaurants_owner", columnNames = "owner_id")
        })
public class JpaRestaurantEntity {

    @Id
    private UUID id;

    @Column
    private String name;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private RestaurantStatus status;

    //US1
    @Column(name = "owner_id", nullable = false, unique = true)
    private UUID ownerId;

    @Setter
    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JpaDishEntity> dishes = new ArrayList<>();

    // US3 fields
    @Column(name = "full_address")
    private String fullAddress;

    @Column(name = "email")
    private String email;

    @Column(name = "opening_hours")
    private String openingHours;

    @Column(name = "logo_url")
    private String logoUrl;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "restaurant_type")
    private RestaurantType restaurantType;

    protected JpaRestaurantEntity() {
    }

    public JpaRestaurantEntity(UUID id, String name, RestaurantStatus status) {
        this.id = id;
        this.name = name;
        this.status = status;
    }






    /*
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
    }*/
    public static JpaRestaurantEntity fromDomain(Restaurant r) {
        JpaRestaurantEntity e = new JpaRestaurantEntity();
        e.id = r.getId().id();
        e.name = r.getName().name();
        e.status = r.getStatus();
        e.ownerId = r.getOwnerId();
        e.fullAddress = r.getFullAddress();
        e.email = r.getEmail();
        e.openingHours = r.getOpeningHours();
        e.logoUrl = r.getLogoUrl();
        e.restaurantType = r.getRestaurantType();
        for (Dish d : r.getDishes()) {
            JpaDishEntity child = JpaDishEntity.fromDomain(d, e);
            e.addDish(child);
        }
        return e;
    }

    public Restaurant toDomain() {
        List<Dish> domainDishes = (this.dishes == null)
                ? new ArrayList<>()
                : this.dishes.stream()
                .map(JpaDishEntity::toDomain)
                .collect(Collectors.toCollection(ArrayList::new)); // mutable

        return new Restaurant(
                new RestaurantId(this.id),
                new RestaurantName(this.name),
                this.status,
                domainDishes,
                this.ownerId,
                this.fullAddress,
                this.email,
                this.openingHours,
                this.logoUrl,
                this.restaurantType
        );
    }

    public void applyFromDomain(Restaurant r) {
        this.name = r.getName().name();
        this.status = r.getStatus();
        this.ownerId = r.getOwnerId();
        this.fullAddress = r.getFullAddress();
        this.email = r.getEmail();
        this.openingHours = r.getOpeningHours();
        this.logoUrl = r.getLogoUrl();
        this.restaurantType = r.getRestaurantType();

        Map<UUID, JpaDishEntity> current = this.dishes.stream()
                .collect(Collectors.toMap(JpaDishEntity::getId, Function.identity()));

        // Upsert children
        for (Dish d : r.getDishes()) {
            UUID did = d.getId().id();
            JpaDishEntity existing = current.get(did);
            if (existing != null) {
                existing.updateFromDomain(d);
            } else {
                JpaDishEntity created = JpaDishEntity.fromDomain(d, this);
                this.addDish(created); // ensures FK restaurant_id is set
            }
        }

        // Remove orphans
        Set<UUID> domainIds = r.getDishes().stream().map(dd -> dd.getId().id()).collect(Collectors.toSet());
        this.dishes.removeIf(j -> !domainIds.contains(j.getId()));
    }

    // Relationship helpers
    public void addDish(JpaDishEntity dish) {
        dish.setRestaurant(this);
        this.dishes.add(dish);
    }

    public void removeDish(JpaDishEntity dish) {
        this.dishes.remove(dish);
        dish.setRestaurant(null);
    }

    // getters
    public UUID getId() { return id; }
    public List<JpaDishEntity> getDishes() { return dishes; }


}
