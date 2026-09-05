package be.kdg.backend.domain.dish;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record DishName(String name) {
    public DishName{
        validate(name);
    }

    public void validate(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Dish name cannot be null or blank");
        }
        if (name.length() < 2 || name.length() > 100) {
            throw new IllegalArgumentException("Dish name must be between 2 and 100 characters");
        }
    }
}
