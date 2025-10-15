package be.kdg.backend.api.dto;

import be.kdg.backend.domain.Price;
import be.kdg.backend.domain.dish.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;
public record DishDto(
        UUID id,
        @NotBlank(message = "A Dish needs a name")
        @Size(min = 2, max = 100, message = "Dish name must be between 2 and 100 characters")
        String name,
        @Size(max = 400, message = "Description cannot be longer than 400 characters")
        String description,
        Price price,
        String category,
        String status
) {
    public static DishDto from(Dish dish) {
        return new DishDto(
                dish.getId().id(),
                dish.getName().name(),
                dish.getDescription().description(),
                dish.getPrice(),
                dish.getCategory().name(),
                dish.getStatus().name()
        );
    }
    public Dish to() {
        return new Dish(
                //TODO: currency nog zien voor beste manier om te initializeren
                DishId.create(),
                new DishName(this.name),
                new Description(this.description),
                new Price(this.price.amount(), "EUR"),
                DishCategory.valueOf(this.category),
                DishStatus.DRAFT
        );
    }
}