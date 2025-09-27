package be.kdg.backend.domain.restaurant;

import be.kdg.backend.domain.NotFoundException;
import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

import java.util.UUID;

@ValueObject
public record RestaurantId(UUID id) {
    public RestaurantId{
        validate();
    }

    public void validate() {
        Assert.notNull(id,"Id cannot be null");
    }

    public NotFoundException notFound() {
        return new NotFoundException("Restaurant with id " + id + " not found");
    }

    public static RestaurantId create() {
        return new RestaurantId(UUID.randomUUID());
    }
}
