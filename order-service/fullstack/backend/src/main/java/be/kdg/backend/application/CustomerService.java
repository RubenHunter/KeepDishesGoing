package be.kdg.backend.application;

import be.kdg.backend.domain.customer.Customer;
import be.kdg.backend.domain.customer.CustomerRepository;
import be.kdg.backend.domain.shared.CustomerId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Application service for the customer profile. Orchestration only — the
 * aggregate owns its own update behavior (coding-mistakes #9).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public Customer saveProfile(UUID customerId, String name, String email, String street,
                                String number, String postalCode, String city, String country) {
        CustomerId id = CustomerId.of(customerId);
        Customer existing = customerRepository.findById(id).orElse(null);
        if (existing == null) {
            log.info("Registering customer profile for {}", customerId);
            return customerRepository.save(Customer.register(id, name, email, street, number, postalCode, city, country));
        }
        existing.updateProfile(name, email, street, number, postalCode, city, country);
        return customerRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public Optional<Customer> getProfile(UUID customerId) {
        return customerRepository.findById(CustomerId.of(customerId));
    }
}
