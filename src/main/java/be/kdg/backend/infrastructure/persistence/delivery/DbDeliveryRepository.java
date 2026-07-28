package be.kdg.backend.infrastructure.persistence.delivery;

import be.kdg.backend.domain.delivery.Delivery;
import be.kdg.backend.domain.delivery.DeliveryRepository;
import be.kdg.backend.domain.shared.DeliveryId;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import be.kdg.backend.domain.shared.OrderId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DbDeliveryRepository implements DeliveryRepository {

    private final SpringDataDeliveryJpaRepository spring;

    @Override public Delivery save(Delivery d) {
        return spring.save(JpaDeliveryEntity.from(d)).toDomain();
    }
    @Override public Optional<Delivery> findById(DeliveryId id) {
        return spring.findById(id.value()).map(JpaDeliveryEntity::toDomain);
    }
    @Override public Optional<Delivery> findByOrderId(OrderId orderId) {
        return spring.findByOrderId(orderId.value()).map(JpaDeliveryEntity::toDomain);
    }
    @Override public List<Delivery> findAvailableForSelfAssignment() {
        return spring.findAvailable().stream().map(JpaDeliveryEntity::toDomain).toList();
    }
    @Override public List<Delivery> findByDeliveryPersonId(DeliveryPersonId id) {
        return spring.findByDeliveryPersonId(id.value()).stream().map(JpaDeliveryEntity::toDomain).toList();
    }
}