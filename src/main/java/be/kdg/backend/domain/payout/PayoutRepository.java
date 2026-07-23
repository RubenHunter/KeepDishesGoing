package be.kdg.backend.domain.payout;

import be.kdg.backend.domain.shared.DeliveryId;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import be.kdg.backend.domain.shared.PayoutId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Repository port for the Payout aggregate. */
public interface PayoutRepository {
    Payout save(Payout payout);
    Optional<Payout> findById(PayoutId id);
    Optional<Payout> findByDeliveryId(DeliveryId deliveryId);
    List<Payout> findByDriverId(DeliveryPersonId driverId);
    List<Payout> findByDriverIdAndDateRange(DeliveryPersonId driverId, LocalDateTime from, LocalDateTime to);
    List<Payout> findByDateRange(LocalDateTime from, LocalDateTime to);
}