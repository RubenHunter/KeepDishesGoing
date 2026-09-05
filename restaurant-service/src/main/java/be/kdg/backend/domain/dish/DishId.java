package be.kdg.backend.domain.dish;

import be.kdg.backend.domain.NotFoundException;
import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

import java.util.UUID;

@ValueObject
public record DishId(UUID id) {
    public DishId{
        validate(id);
    }

    public void validate(UUID id) {
        Assert.notNull(id,"Id cannot be null");
    }

    public NotFoundException notFound() {
        return new NotFoundException("Dish with id " + id + " not found");
    }

    public static DishId create() {
        return new DishId(UUID.randomUUID());
    }
}
