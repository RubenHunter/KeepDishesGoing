package be.kdg.backend.domain.restaurant;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

@ValueObject
public record RestaurantName(String name) {
    public RestaurantName {
        validate(name);
    }

    public void validate(String name) {
        Assert.notNull(name, "Name cannot be null");
        Assert.hasText(name, "Name cannot be empty");
        //other validation here if needed.
    }
}
