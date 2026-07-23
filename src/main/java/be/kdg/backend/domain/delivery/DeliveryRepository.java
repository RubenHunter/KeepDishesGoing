package be.kdg.backend.domain.delivery;

import be.kdg.backend.domain.shared.DeliveryId;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import be.kdg.backend.domain.shared.OrderId;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository {
    Delivery save(Delivery delivery);
    Optional<Delivery> findById(DeliveryId id);
    Optional<Delivery> findByOrderId(OrderId orderId);
    List<Delivery> findAvailableForSelfAssignment();
    List<Delivery> findByDeliveryPersonId(DeliveryPersonId id);
}