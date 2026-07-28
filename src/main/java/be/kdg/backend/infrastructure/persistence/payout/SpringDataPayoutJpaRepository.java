package be.kdg.backend.infrastructure.persistence.payout;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataPayoutJpaRepository extends JpaRepository<JpaPayoutEntity, UUID> {

    Optional<JpaPayoutEntity> findByDeliveryId(UUID deliveryId);
    List<JpaPayoutEntity> findByDriverId(UUID driverId);

    @Query("select e from JpaPayoutEntity e where e.driverId = :driverId and e.computedAt >= :from and e.computedAt <= :to")
    List<JpaPayoutEntity> findByDriverIdAndRange(UUID driverId, LocalDateTime from, LocalDateTime to);

    @Query("select e from JpaPayoutEntity e where e.computedAt >= :from and e.computedAt <= :to")
    List<JpaPayoutEntity> findByDateRange(LocalDateTime from, LocalDateTime to);
}