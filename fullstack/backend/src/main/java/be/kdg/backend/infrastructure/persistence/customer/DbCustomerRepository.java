package be.kdg.backend.infrastructure.persistence.customer;

import be.kdg.backend.domain.customer.Customer;
import be.kdg.backend.domain.customer.CustomerRepository;
import be.kdg.backend.domain.shared.CustomerId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Infrastructure-side adapter implementing the domain {@link CustomerRepository} port. */
@Repository
@RequiredArgsConstructor
public class DbCustomerRepository implements CustomerRepository {

    private final SpringDataCustomerJpaRepository spring;

    @Override
    public Customer save(Customer customer) {
        JpaCustomerEntity saved = spring.save(JpaCustomerEntity.fromDomain(customer));
        return saved.toDomain();
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return spring.findById(id.value()).map(JpaCustomerEntity::toDomain);
    }
}
