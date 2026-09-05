package be.kdg.backend.infrastructure.persistence.driver;

import be.kdg.backend.domain.driver.DeliveryPerson;
import be.kdg.backend.domain.driver.DeliveryPersonRepository;
import be.kdg.backend.domain.shared.DeliveryId;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DbDeliveryPersonRepository implements DeliveryPersonRepository {

    private final SpringDataDeliveryPersonJpaRepository spring;

    @Override public DeliveryPerson save(DeliveryPerson p) {
        return spring.save(JpaDeliveryPersonEntity.from(p)).toDomain();
    }
    @Override public Optional<DeliveryPerson> findById(DeliveryPersonId id) {
        return spring.findById(id.value()).map(JpaDeliveryPersonEntity::toDomain);
    }
    @Override public Optional<DeliveryPerson> findByAssignedDeliveryId(DeliveryId deliveryId) {
        return spring.findByAssignedDeliveryId(deliveryId.value()).map(JpaDeliveryPersonEntity::toDomain);
    }
}