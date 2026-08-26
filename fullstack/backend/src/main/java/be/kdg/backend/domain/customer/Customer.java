package be.kdg.backend.domain.customer;

import be.kdg.backend.domain.shared.CustomerId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.util.Objects;

/**
 * Customer aggregate root — stores the account profile (name, contact email,
 * home address) keyed by the Keycloak subject so it follows the user across devices.
 */
@AggregateRoot
public class Customer {

    @Identity
    private final CustomerId id;

    private String name;
    private String email;
    private String street;
    private String number;
    private String postalCode;
    private String city;
    private String country;

    private Customer(CustomerId id, String name, String email, String street, String number,
                     String postalCode, String city, String country) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.street = street;
        this.number = number;
        this.postalCode = postalCode;
        this.city = city;
        this.country = country;
    }

    public static Customer register(CustomerId id, String name, String email, String street,
                                    String number, String postalCode, String city, String country) {
        Objects.requireNonNull(id, "id");
        Customer customer = new Customer(id, "", "", "", "", "", "", "");
        customer.updateProfile(name, email, street, number, postalCode, city, country);
        return customer;
    }

    public static Customer rehydrate(CustomerId id, String name, String email, String street,
                                     String number, String postalCode, String city, String country) {
        Objects.requireNonNull(id, "id");
        return new Customer(id, name, email, street, number, postalCode, city, country);
    }

    /** Replaces the whole profile. Blank fields are tolerated (contact info, not an invariant). */
    public void updateProfile(String name, String email, String street, String number,
                              String postalCode, String city, String country) {
        this.name = trim(name);
        this.email = trim(email);
        this.street = trim(street);
        this.number = trim(number);
        this.postalCode = trim(postalCode);
        this.city = trim(city);
        this.country = trim(country);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    public CustomerId id() { return id; }
    public String name() { return name; }
    public String email() { return email; }
    public String street() { return street; }
    public String number() { return number; }
    public String postalCode() { return postalCode; }
    public String city() { return city; }
    public String country() { return country; }
}
