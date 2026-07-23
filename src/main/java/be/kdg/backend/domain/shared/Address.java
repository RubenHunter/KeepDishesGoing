package be.kdg.backend.domain.shared;

import java.util.Objects;

/** Postal address value object. */
public record Address(String street, String number, String postalCode, String city, String country) {
    public Address {
        requireNonBlank("street", street);
        requireNonBlank("number", number);
        requireNonBlank("postalCode", postalCode);
        requireNonBlank("city", city);
        requireNonBlank("country", country);
    }
    private static void requireNonBlank(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
    public String singleLine() {
        return street + " " + number + ", " + postalCode + " " + city + ", " + country;
    }
}