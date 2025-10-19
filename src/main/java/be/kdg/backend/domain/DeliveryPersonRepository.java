package be.kdg.backend.domain;

import org.jmolecules.ddd.annotation.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryPersonRepository {
    Optional<DeliveryPerson> findById(DeliveryPersonId id);
    List<DeliveryPerson> findByIsAvailable(boolean isAvailable);
    List<DeliveryPerson> findAll();
    DeliveryPerson save(DeliveryPerson deliveryPerson);
    void delete(DeliveryPerson deliveryPerson);
    boolean existsById(DeliveryPersonId id);
}