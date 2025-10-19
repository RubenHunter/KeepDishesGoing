package be.kdg.backend.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

@ValueObject
public record Address(String street, String city, String postalCode, String country) {
    public Address {
        validate(street, city, postalCode, country);
    }

    private void validate(String street, String city, String postalCode, String country) {
        Assert.hasText(street, "Street cannot be null or empty");
        Assert.hasText(city, "City cannot be null or empty");
        Assert.hasText(postalCode, "Postal code cannot be null or empty");
        Assert.hasText(country, "Country cannot be null or empty");

        if (street.length() > 100) {
            throw new IllegalArgumentException("Street cannot be longer than 100 characters");
        }
        if (city.length() > 50) {
            throw new IllegalArgumentException("City cannot be longer than 50 characters");
        }
        if (postalCode.length() > 20) {
            throw new IllegalArgumentException("Postal code cannot be longer than 20 characters");
        }
    }

    public String getFullAddress() {
        return String.format("%s, %s %s, %s", street, postalCode, city, country);
    }
}