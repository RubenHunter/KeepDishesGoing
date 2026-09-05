package be.kdg.backend.domain.shared;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Postal address value object. Domain-only, no JPA.
 */
public final class Address {
    private static final Pattern POSTAL_CODE_NON_BLANK = Pattern.compile(".+");

    private final String street;
    private final String number;
    private final String postalCode;
    private final String city;
    private final String country;

    public Address(String street, String number, String postalCode, String city, String country) {
        requireNonBlank("street", street);
        requireNonBlank("number", number);
        requireNonBlank("postalCode", postalCode);
        requireNonBlank("city", city);
        requireNonBlank("country", country);
        this.street = street;
        this.number = number;
        this.postalCode = postalCode;
        this.city = city;
        this.country = country;
    }

    public String street()     { return street; }
    public String number()      { return number; }
    public String postalCode() { return postalCode; }
    public String city()       { return city; }
    public String country()    { return country; }

    public String singleLine() {
        return street + " " + number + ", " + postalCode + " " + city + ", " + country;
    }

    private static void requireNonBlank(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank (Address)");
        }
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return street.equals(address.street)
                && number.equals(address.number)
                && postalCode.equals(address.postalCode)
                && city.equals(address.city)
                && country.equals(address.country);
    }

    @Override public int hashCode() { return Objects.hash(street, number, postalCode, city, country); }

    @Override public String toString() { return singleLine(); }
}