package be.kdg.backend.infrastructure.persistence.customer;

import be.kdg.backend.domain.customer.Customer;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** JPA entity for the {@link Customer} aggregate. Stored in {@code ordering.customers}. */
@Entity
@Table(name = "customers", schema = "ordering")
public class JpaCustomerEntity {

    @Id
    private UUID id;

    private String name;
    private String email;
    private String street;
    private String number;
    private String postalCode;
    private String city;
    private String country;

    public JpaCustomerEntity() {}

    public static JpaCustomerEntity fromDomain(Customer c) {
        JpaCustomerEntity e = new JpaCustomerEntity();
        e.id = c.id().value();
        e.name = c.name();
        e.email = c.email();
        e.street = c.street();
        e.number = c.number();
        e.postalCode = c.postalCode();
        e.city = c.city();
        e.country = c.country();
        return e;
    }

    public Customer toDomain() {
        return Customer.rehydrate(
                be.kdg.backend.domain.shared.CustomerId.of(id),
                name, email, street, number, postalCode, city, country);
    }

    public UUID getId() { return id; }
}
