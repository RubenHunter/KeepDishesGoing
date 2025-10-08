package be.kdg.backend.domain.restaurant;

import be.kdg.backend.domain.Price;
import be.kdg.backend.domain.dish.*;
import lombok.*;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

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
    //createDraftDish(DishName dish, Price price) : DishId  -> business rule: Dish starts as DRAFT (DishStatus), must be explicitly published to be visible on menu
    public DishId createDraftDish(DishName name, Price price) {
        DishId newId = DishId.create();
        Dish draftDish = new Dish(
                newId,
                name,
                new Description(""), // or require description as param
                price,
                DishCategory.MAIN_COURSE, // or require category as param
                DishStatus.DRAFT
        );
        dishes.add(draftDish);
        return newId;
    }

    // Update only mutable fields; keep name immutable
    public void updateDraftDish(DishId dishId, String name, String description, Price price, DishCategory category) {
        Dish dish = findDishById(dishId);

        // Name is immutable; ignore or validate if provided
        // if (name != null && !name.isBlank() && !new DishName(name).equals(dish.getName())) {
        //     throw new IllegalArgumentException("Renaming a dish is not supported");
        // }

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
