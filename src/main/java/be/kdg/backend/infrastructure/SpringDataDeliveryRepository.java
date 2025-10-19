package be.kdg.backend.infrastructure;

import be.kdg.backend.domain.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataDeliveryRepository extends JpaRepository<DeliveryJpaEntity, String> {

    @Query("SELECT d FROM DeliveryJpaEntity d WHERE d.deliveryPersonId = :deliveryPersonId")
    List<DeliveryJpaEntity> findByDeliveryPersonId(@Param("deliveryPersonId") String deliveryPersonId);

    List<DeliveryJpaEntity> findByStatus(String status);

    @Query("SELECT d FROM DeliveryJpaEntity d WHERE d.status = :status")
    List<DeliveryJpaEntity> findByStatus(@Param("status") DeliveryStatus status);

    @Query("SELECT d FROM DeliveryJpaEntity d WHERE d.status = 'PENDING' AND d.availableForSelfAssignment = true AND d.deliveryPersonId IS NULL")
    List<DeliveryJpaEntity> findAvailableForAssignment();
}