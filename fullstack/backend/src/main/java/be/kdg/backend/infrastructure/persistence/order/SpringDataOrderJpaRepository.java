package be.kdg.backend.infrastructure.persistence.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataOrderJpaRepository extends JpaRepository<JpaOrderEntity, UUID> {

    @Query("select e from JpaOrderEntity e where e.paymentRef = :ref")
    Optional<JpaOrderEntity> findByPaymentRef(String ref);

    List<JpaOrderEntity> findByCustomerId(UUID customerId);
}