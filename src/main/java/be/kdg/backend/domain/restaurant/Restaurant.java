package be.kdg.backend.domain.restaurant;

import be.kdg.backend.domain.Price;
import be.kdg.backend.domain.dish.*;
import lombok.*;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
@ToString
@AggregateRoot
public class Restaurant {

    @Identity
    private final RestaurantId id;
    private final RestaurantName name;
    private RestaurantStatus status;
    //private Address address;
    private List<Dish> dishes;

    /*
    Constructor en getters zijn al gegenereerd door Lombok annotaties
    public Restaurant(final RestaurantId id, final RestaurantName name, final RestaurantStatus status, final List<Dish> dishes) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.dishes = dishes;
    }
    */

    // Static factory to enforce domain creation rules
    public static Restaurant create(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Restaurant name must not be blank");
        }
        return new Restaurant(
                RestaurantId.create(),
                new RestaurantName(name),
                RestaurantStatus.INACTIVE,
                new ArrayList<>()
        );
    }

    public void open() {
        if (this.status == RestaurantStatus.ACTIVE) {
            throw new IllegalStateException("Restaurant is already open");
        }
        this.status = RestaurantStatus.ACTIVE;
    }

    public void close() {
        if (this.status == RestaurantStatus.INACTIVE) {
            throw new IllegalStateException("Restaurant is already closed");
        }
        this.status = RestaurantStatus.INACTIVE;
    }


    //methodes:
    //make dublicate wanneer je een published dish update en draft maakt.
    //createDraftDish(DishName dish, Price price) : DishId  -> business rule: Dish starts as DRAFT (DishStatus), must be explicitly published to be visible on menu
    public DishId createDraftDish(DishName name, Description description, DishCategory category, Price price) {
        //Use create static factory
        Dish draftDish = Dish.createDraft(name, description, price, category);
        dishes.add(draftDish);
        return draftDish.getId();
    }

    // Update only mutable fields; keep name immutable
    public void updateDraftDish(DishId dishId, String name, String description, Price price, DishCategory category) {
        Dish dish = findDishById(dishId);
        if (description != null) {
            dish.updateDescription(new Description(description));
        }
        if (price != null) {
            dish.updatePrice(price);
        }
        if (category != null) {
            dish.updateCategory(category);
        }
        dish.markAsDraft();
    }


    //publishDish(DishId dishId)
    public void publishDish(DishId dishId) {
        Dish dish = findDishById(dishId);
        validateDishCanBePublished(dish);
        dish.publish();
    }
    public void dePublishDish(DishId dishId) {
        Dish dish = findDishById(dishId);
        dish.markAsDraft();
    }

    //markDishOutOfStock(DishId dishId)
    public void markDishOutOfStock(DishId dishId) {
        Dish dish = findDishById(dishId);
        dish.markOutOfStock();
    }

    //updateDishPrice(DishId dishId, Price newPrice)
    public void updateDishPrice(DishId dishId, Price newPrice) {
        Dish dish = findDishById(dishId);
        dish.updatePrice(newPrice);
    }

    //getPublishedMenu(): List<Dish>  -> only return dishes with DishStatus PUBLISHED
    public List<Dish> getPublishedMenu() {
        return dishes.stream()
                .filter(d -> d.getStatus() == DishStatus.PUBLISHED)
                .toList();
    }

    //validateDishCanBePublished(Dish dish)
    public void validateDishCanBePublished(Dish dish) {
        if (dish.getStatus() == DishStatus.PUBLISHED) {
            throw new IllegalStateException("Dish is already published");
        }
        /*
        if (dish.getStatus() == DishStatus.OUT_OF_STOCK) {
            throw new IllegalStateException("Cannot publish dish that is out of stock");
        }
        */

    }

    // Helper method
    private Dish findDishById(DishId dishId) {
        return dishes.stream()
                .filter(d -> d.getId().equals(dishId))
                .findFirst()
                .orElseThrow(() -> dishId.notFound());
    }

    public Dish getDishById(DishId dishId) {
        return findDishById(dishId);
    }

}
