package be.kdg.backend.domain.restaurant;

import be.kdg.backend.domain.DomainConflictException;
import be.kdg.backend.domain.Price;
import be.kdg.backend.domain.dish.*;
import lombok.*;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
            throw new DomainConflictException("Restaurant is already open");
        }
        this.status = RestaurantStatus.ACTIVE;
    }

    public void close() {
        if (this.status == RestaurantStatus.INACTIVE) {
            throw new DomainConflictException("Restaurant is already closed");
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


    // Update behavior with versioning:
    // Reuse a single draft per logical dish (by name). If editing a published dish:
    // - find existing draft with same name and reset it to the current published state,
    // - If target is PUBLISHED -> create a new DRAFT copy with updates and return its id.
    // - If target is DRAFT/other -> update in place and keep it DRAFT.
    // Name is immutable; reject attempts to change it.
    public DishId updateDraftDish(DishId dishId, String name, String description, Price price, DishCategory category) {
        Dish current = findDishById(dishId);

        if (name != null && !name.equals(current.getName().name())) {
            throw new IllegalArgumentException("Dish name is immutable");
        }

        if (current.getStatus() == DishStatus.PUBLISHED) {
            Dish draft = findDraftByName(current.getName())
                    .orElseGet(() -> {
                        Dish d = Dish.createDraft(current.getName(), current.getDescription(), current.getPrice(), current.getCategory());
                        dishes.add(d);
                        return d;
                    });

            // reset draft to current published baseline
            draft.updateDescription(current.getDescription());
            draft.updatePrice(current.getPrice());
            draft.updateCategory(current.getCategory());

            // apply incoming updates
            if (description != null) {
                draft.updateDescription(new Description(description));
            }
            if (price != null) {
                draft.updatePrice(price);
            }
            if (category != null) {
                draft.updateCategory(category);
            }
            draft.markAsDraft();
            return draft.getId();
        } else {
            if (description != null) {
                current.updateDescription(new Description(description));
            }
            if (price != null) {
                current.updatePrice(price);
            }
            if (category != null) {
                current.updateCategory(category);
            }
            current.markAsDraft();
            return dishId;
        }
    }


    //publishDish(DishId dishId)
    // Publish a dish and delete any other published version with the same name
    public void publishDish(DishId dishId) {
        Dish toPublish = findDishById(dishId);
        validateDishCanBePublished(toPublish);

        // Remove previously published version(s) with the same name
        dishes.removeIf(other ->
                !other.getId().equals(toPublish.getId())
                        && other.getStatus() == DishStatus.PUBLISHED
                        && other.getName().equals(toPublish.getName())
        );

        toPublish.publish();
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
            throw new DomainConflictException("Dish is already published");
        }
    }

    private Optional<Dish> findDraftByName(DishName name) {
        return dishes.stream()
                .filter(d -> d.getStatus() == DishStatus.DRAFT && d.getName().equals(name))
                .findFirst();
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
