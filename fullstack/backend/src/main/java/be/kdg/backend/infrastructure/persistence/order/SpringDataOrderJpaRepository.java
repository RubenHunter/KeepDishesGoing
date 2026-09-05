package be.kdg.backend.infrastructure.persistence.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataOrderJpaRepository extends JpaRepository<JpaOrderEntity, UUID> {

    @Query("select e from JpaOrderEntity e where e.paymentRef = :ref")
    Optional<JpaOrderEntity> findByPaymentRef(String ref);

    List<JpaOrderEntity> findByCustomerId(UUID customerId);

    @Query("select e from JpaOrderEntity e where e.status = 'PLACED' and e.placedAt < :before")
    List<JpaOrderEntity> findPlacedBefore(@Param("before") LocalDateTime before);

    @Query("select e from JpaOrderEntity e where e.restaurantId = :restaurantId order by e.createdAt desc")
    List<JpaOrderEntity> findByRestaurantId(@Param("restaurantId") UUID restaurantId);
}