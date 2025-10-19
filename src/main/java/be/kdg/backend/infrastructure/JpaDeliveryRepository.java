package be.kdg.backend.infrastructure;

import be.kdg.backend.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaDeliveryRepository implements DeliveryRepository {
    private final SpringDataDeliveryRepository springDataRepository;

    @Override
    public Optional<Delivery> findById(DeliveryId id) {
        return springDataRepository.findById(id.value())
                .map(DeliveryJpaEntity::toDomain);
    }

    @Override
    public List<Delivery> findByDeliveryPersonId(DeliveryPersonId deliveryPersonId) {
        return springDataRepository.findByDeliveryPersonId(deliveryPersonId.value()).stream()
                .map(DeliveryJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Delivery> findByStatus(DeliveryStatus status) {
        return springDataRepository.findByStatus(status).stream()
                .map(DeliveryJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Delivery> findAll() {
        return springDataRepository.findAll().stream()
                .map(DeliveryJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Delivery save(Delivery delivery) {
        DeliveryJpaEntity entity = DeliveryJpaEntity.fromDomain(delivery);
        DeliveryJpaEntity saved = springDataRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void delete(Delivery delivery) {
        springDataRepository.deleteById(delivery.getId().value());
    }

    @Override
    public boolean existsById(DeliveryId id) {
        return springDataRepository.existsById(id.value());
    }
}