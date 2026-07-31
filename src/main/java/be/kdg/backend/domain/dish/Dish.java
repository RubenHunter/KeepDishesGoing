package be.kdg.backend.domain.dish;

import be.kdg.backend.domain.Price;
import be.kdg.backend.domain.ValidationException;
import lombok.*;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

@Getter
@AllArgsConstructor
@ToString
@Entity
public class Dish {
    @Identity
    private final DishId id;
    private final DishName name;
    private Description description;
    private Price price;
    private DishCategory category;
    private DishStatus status;
    private String imageUrl;

    // Static factory to enforce domain creation rules
    public static Dish createDraft(DishName name, Description description, Price price, DishCategory category, String imageUrl) {
        if (name == null) {
            throw new ValidationException("Dish name must not be null");
        }
        if (price == null) {
            throw new ValidationException("Price must not be null");
        }
        validatePriceStatic(price);

        Description safeDescription = (description != null) ? description : new Description("");
        DishCategory safeCategory = (category != null) ? category : DishCategory.MAIN_COURSE;

        return new Dish(
                DishId.create(),
                name,
                safeDescription,
                price,
                safeCategory,
                DishStatus.DRAFT,
                imageUrl
        );
    }

    //methods
    //publish() -> validation: Price must be positive, Dish cannot be already published
    public void publish() {
        if (this.status == DishStatus.PUBLISHED) {
            throw new ValidationException("Dish is already published");
        }
        /*
        if (this.status == DishStatus.OUT_OF_STOCK) {
            throw new IllegalStateException("Cannot publish dish that is out of stock");
        }

         */
        validatePrice(this.price);
        this.status = DishStatus.PUBLISHED;
    }


    public void markOutOfStock() {
        this.status = DishStatus.OUT_OF_STOCK;
    }

    public void markAsDraft() {
        this.status = DishStatus.DRAFT;
    }

    public void updatePrice(Price newPrice) {
        validatePrice(newPrice);
        this.price = newPrice;
    }

    public void updateDescription(Description newDescription) {
        this.description = newDescription;
    }

    public void updateCategory(DishCategory newCategory) {
        this.category = newCategory;
    }

    public void updateImageUrl(String newImageUrl) {
        this.imageUrl = newImageUrl;
    }

    public boolean isAvailable(Price price) {
        return this.status == DishStatus.PUBLISHED && this.price.isPositive(price.amount());
    }

    private void validatePrice(Price price) {
        if (!price.isPositive(price.amount())) {
            throw new ValidationException("Price must be positive");
        }
    }

    //Static version for the static create factory
    private static void validatePriceStatic(Price price) {
        if (!price.isPositive(price.amount())) {
            throw new ValidationException("Price must be positive");
        }
    }
}
