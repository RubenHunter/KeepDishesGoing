package be.kdg.backend.domain.customer;

import be.kdg.backend.domain.shared.CustomerId;

import java.util.Optional;

/** Repository port for the {@link Customer} aggregate root. Owned by domain; implemented in infrastructure. */
public interface CustomerRepository {
    Customer save(Customer customer);
    Optional<Customer> findById(CustomerId id);
}
