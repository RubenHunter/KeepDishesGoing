package be.kdg.backend.infrastructure.persistence.customer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataCustomerJpaRepository extends JpaRepository<JpaCustomerEntity, UUID> {
}
