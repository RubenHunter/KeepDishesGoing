package be.kdg.backend.domain.dish;

import be.kdg.backend.domain.Price;
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


    //methods
    //publish() -> validation: Price must be positive, Dish cannot be empty
    public void publish() {
        if (this.status == DishStatus.PUBLISHED) {
            throw new IllegalStateException("Dish is already published");
        }
        if (this.status == DishStatus.OUT_OF_STOCK) {
            throw new IllegalStateException("Cannot publish dish that is out of stock");
        }
        validatePrice(this.price);
        this.status = DishStatus.PUBLISHED;
    }

    //markOutOfStock()
    public void markOutOfStock() {
        this.status = DishStatus.OUT_OF_STOCK;
    }

    //updatePrice(Price newPrice)
    public void updatePrice(Price newPrice) {
        validatePrice(newPrice);
        this.price = newPrice;
    }

    //isAvailable()
    public boolean isAvailable() {
        return this.status == DishStatus.PUBLISHED && this.price.isPositive();
    }

    //validatePrice(Price price)
    private void validatePrice(Price price) {
        if (!price.isPositive()) {
            throw new IllegalArgumentException("Price must be positive");
        }
    }
}
