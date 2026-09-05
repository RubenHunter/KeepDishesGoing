package be.kdg.backend.infrastructure.jpa;

import be.kdg.backend.domain.Price;
import be.kdg.backend.domain.dish.*;
import jakarta.persistence.*;
import lombok.Getter;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "dishes")
public class JpaDishEntity {

    @Id
    private UUID id;

    @Getter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private JpaRestaurantEntity restaurant;

    @Column
    private String name;

    @Column
    private String description;

    //TODO: not sure yet how to do this, maybe 2 fields or just 1 string?
    @Getter
    @Column
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column
    private DishCategory category;

    @Enumerated(EnumType.STRING)
    @Column
    private DishStatus status;

    @Column(name = "image_url")
    private String imageUrl;

    protected JpaDishEntity() {
    }

    public JpaDishEntity(UUID id, String name, String description, BigDecimal price, DishCategory category, DishStatus status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.status = status;
    }

    public static JpaDishEntity fromDomain(Dish d, JpaRestaurantEntity restaurant) {
        JpaDishEntity e = new JpaDishEntity();
        e.id = d.getId().id();
        e.name = d.getName().name();
        e.description = d.getDescription() == null ? null : d.getDescription().description();
        e.price = d.getPrice().amount();
        e.category = d.getCategory();
        e.status = d.getStatus();
        e.imageUrl = d.getImageUrl();
        e.setRestaurant(restaurant);
        return e;
    }

    public Dish toDomain() {
        return new Dish(
                new DishId(id),
                new DishName(name),
                new Description(description),
                new Price(price, "EUR"),
                category,
                status,
                imageUrl
        );
    }

    // Update mutable fields only
    public void updateFromDomain(Dish d) {
        this.description = d.getDescription() != null ? d.getDescription().description() : null;
        this.price = d.getPrice() != null ? d.getPrice().amount() : null;
        this.category = d.getCategory();
        this.status = d.getStatus();
        this.imageUrl = d.getImageUrl();
        // name stays immutable by domain rule
    }

    public UUID getId() { return id; }
    public void setRestaurant(JpaRestaurantEntity restaurant) { this.restaurant = restaurant; }

}
