package be.kdg.backend.infrastructure.persistence.payout;

import be.kdg.backend.domain.payout.Payout;
import be.kdg.backend.domain.payout.PayoutRepository;
import be.kdg.backend.domain.shared.DeliveryId;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import be.kdg.backend.domain.shared.PayoutId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DbPayoutRepository implements PayoutRepository {

    private final SpringDataPayoutJpaRepository spring;

    @Override public Payout save(Payout p) {
        return spring.save(JpaPayoutEntity.from(p)).toDomain();
    }
    @Override public Optional<Payout> findById(PayoutId id) {
        return spring.findById(java.util.UUID.fromString(id.value().toString())).map(JpaPayoutEntity::toDomain);
    }
    @Override public Optional<Payout> findByDeliveryId(DeliveryId deliveryId) {
        return spring.findByDeliveryId(deliveryId.value()).map(JpaPayoutEntity::toDomain);
    }
    @Override public List<Payout> findByDriverId(DeliveryPersonId driverId) {
        return spring.findByDriverId(driverId.value()).stream().map(JpaPayoutEntity::toDomain).toList();
    }
    @Override public List<Payout> findByDriverIdAndDateRange(DeliveryPersonId driverId, LocalDateTime from, LocalDateTime to) {
        return spring.findByDriverIdAndRange(driverId.value(), from, to).stream().map(JpaPayoutEntity::toDomain).toList();
    }
    @Override public List<Payout> findByDateRange(LocalDateTime from, LocalDateTime to) {
        return spring.findByDateRange(from, to).stream().map(JpaPayoutEntity::toDomain).toList();
    }
}