package be.kdg.backend.infrastructure.persistence.delivery;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataDeliveryJpaRepository extends JpaRepository<JpaDeliveryEntity, UUID> {

    Optional<JpaDeliveryEntity> findByOrderId(UUID orderId);
    List<JpaDeliveryEntity> findByDeliveryPersonId(UUID driverId);

    @Query("select e from JpaDeliveryEntity e where e.deliveryPersonId is null and (e.status = be.kdg.backend.domain.delivery.DeliveryStatus.PENDING or e.status = be.kdg.backend.domain.delivery.DeliveryStatus.READY_FOR_PICKUP)")
    List<JpaDeliveryEntity> findAvailable();
}