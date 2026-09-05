package be.kdg.backend.infrastructure.persistence.driver;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataDeliveryPersonJpaRepository extends JpaRepository<JpaDeliveryPersonEntity, UUID> {
    Optional<JpaDeliveryPersonEntity> findByAssignedDeliveryId(UUID deliveryId);
}