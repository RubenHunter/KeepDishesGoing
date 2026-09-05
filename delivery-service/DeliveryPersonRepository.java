package be.kdg.backend.domain.driver;

import be.kdg.backend.domain.shared.DeliveryPersonId;

import java.util.Optional;

public interface DeliveryPersonRepository {
    DeliveryPerson save(DeliveryPerson person);
    Optional<DeliveryPerson> findById(DeliveryPersonId id);
    Optional<DeliveryPerson> findByAssignedDeliveryId(be.kdg.backend.domain.shared.DeliveryId deliveryId);
}