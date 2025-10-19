package be.kdg.backend.domain;

import org.jmolecules.ddd.annotation.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository {
    Optional<Delivery> findById(DeliveryId id);
    List<Delivery> findByDeliveryPersonId(DeliveryPersonId deliveryPersonId);
    List<Delivery> findByStatus(DeliveryStatus status);
    List<Delivery> findAll();
    Delivery save(Delivery delivery);
    void delete(Delivery delivery);
    boolean existsById(DeliveryId id);
}