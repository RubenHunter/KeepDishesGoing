package be.kdg.sa.backend.domain.Entities;

import lombok.Value;
import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;
import java.util.UUID;

@ValueObject
@Value
public class MenuItemId {
    String value;

    private MenuItemId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("MenuItem ID cannot be null or empty");
        }
        this.value = value;
    }

    public static MenuItemId of(String value) {
        return new MenuItemId(value);
    }

    public static MenuItemId generate() {
        return new MenuItemId("MENU-" + UUID.randomUUID().toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MenuItemId that = (MenuItemId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    public String getValue() {
        return value;
    }
}