package be.kdg.backend.infrastructure.persistence.tracking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataOrderEventJpaRepository extends JpaRepository<JpaOrderEventEntity, Long> {
    List<JpaOrderEventEntity> findByOrderIdOrderByOccurredAt(UUID orderId);
}