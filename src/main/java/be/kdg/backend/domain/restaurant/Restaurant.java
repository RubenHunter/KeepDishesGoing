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
import java.util.UUID;

@Getter
@AllArgsConstructor
@ToString
@AggregateRoot
public class Restaurant {

    @Identity
    private final RestaurantId id;
    private final RestaurantName name;
    private RestaurantStatus status;
    private List<Dish> dishes;
    private UUID ownerId;

    // US3 required fields
    private String fullAddress;
    private String email;
    private String openingHours;
    private String logoUrl;

    public Restaurant(RestaurantId id, RestaurantName name, RestaurantStatus status, List<Dish> dishes) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.dishes = dishes;
        this.ownerId = null;
        this.fullAddress = null;
        this.email = null;
        this.openingHours = null;
        this.logoUrl = null;
    }

    // Static factory to enforce domain creation rules
    public static Restaurant create(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Restaurant name must not be blank");
        }
        return new Restaurant(
                RestaurantId.create(),
                new RestaurantName(name),
                RestaurantStatus.INACTIVE,
                new ArrayList<>(),
                null,
                null, null, null, null
        );
    }

    public static Restaurant create(String name, UUID ownerId) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Restaurant name must not be blank");
        if (ownerId == null) throw new IllegalArgumentException("ownerId must not be null");
        return new Restaurant(RestaurantId.create(), new RestaurantName(name), RestaurantStatus.INACTIVE, new ArrayList<>(), ownerId, null, null, null, null);
    }

    // US3: full required data on create
    public static Restaurant create(String name,
                                    String fullAddress,
                                    String email,
                                    String openingHours,
                                    String logoUrl,
                                    UUID ownerId) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Restaurant name must not be blank");
        if (fullAddress == null || fullAddress.isBlank()) throw new IllegalArgumentException("fullAddress must not be blank");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("email must not be blank");
        if (openingHours == null || openingHours.isBlank()) throw new IllegalArgumentException("openingHours must not be blank");
        if (logoUrl == null || logoUrl.isBlank()) throw new IllegalArgumentException("logoUrl must not be blank");
        if (ownerId == null) throw new IllegalArgumentException("ownerId must not be null");

        return new Restaurant(
                RestaurantId.create(),
                new RestaurantName(name),
                RestaurantStatus.INACTIVE,
                new ArrayList<>(),
                ownerId,
                fullAddress,
                email,
                openingHours,
                logoUrl
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


    // Publish a dish and delete any other published version with the same name
    // US10: publish with cap of 10 published dishes; allow replacement by name
    public void publishDish(DishId dishId) {
        Dish toPublish = findDishById(dishId);
        if (toPublish.getStatus() == DishStatus.PUBLISHED) {
            throw new DomainConflictException("Dish is already published");
        }

        boolean replacing = dishes.stream()
                .anyMatch(other -> other.getStatus() == DishStatus.PUBLISHED && other.getName().equals(toPublish.getName()));

        long publishedCount = dishes.stream().filter(d -> d.getStatus() == DishStatus.PUBLISHED).count();
        if (!replacing && publishedCount >= 10) {
            throw new DomainConflictException("Maximum of 10 dishes can be published");
        }

        // Replace any published version with the same name
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

    public void markDishOutOfStock(DishId dishId) {
        Dish dish = findDishById(dishId);
        dish.markOutOfStock();
    }

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

    public void validateDishCanBePublished(Dish dish) {
        if (dish.getStatus() == DishStatus.PUBLISHED) {
            throw new DomainConflictException("Dish is already published");
        }
    }

    // US10 + domain-level bulk publish (no looping in service)
    public void publishAllDraftDishes() {
        long publishedCount = dishes.stream().filter(d -> d.getStatus() == DishStatus.PUBLISHED).count();

        // Publish drafts in-place while respecting cap and allowing replacements
        for (Dish draft : new ArrayList<>(dishes)) {
            if (draft.getStatus() != DishStatus.DRAFT) continue;

            boolean replacing = dishes.stream()
                    .anyMatch(other -> other.getStatus() == DishStatus.PUBLISHED && other.getName().equals(draft.getName()));

            if (!replacing && publishedCount >= 10) {
                // skip this draft; cap reached and no replacement
                continue;
            }

            // Replace published with same name and publish draft
            dishes.removeIf(other ->
                    !other.getId().equals(draft.getId())
                            && other.getStatus() == DishStatus.PUBLISHED
                            && other.getName().equals(draft.getName())
            );

            draft.publish();

            if (!replacing) {
                publishedCount++;
            }
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
