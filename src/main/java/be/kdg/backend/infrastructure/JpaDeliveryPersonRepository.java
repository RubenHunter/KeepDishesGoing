package be.kdg.backend.infrastructure;

import be.kdg.backend.domain.DeliveryPerson;
import be.kdg.backend.domain.DeliveryPersonId;
import be.kdg.backend.domain.DeliveryPersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaDeliveryPersonRepository implements DeliveryPersonRepository {
    private final SpringDataDeliveryPersonRepository springDataRepository;

    @Override
    public Optional<DeliveryPerson> findById(DeliveryPersonId id) {
        return springDataRepository.findById(id.value())
                .map(DeliveryPersonJpaEntity::toDomain);
    }

    @Override
    public List<DeliveryPerson> findByIsAvailable(boolean isAvailable) {
        return springDataRepository.findByIsAvailable(isAvailable).stream()
                .map(DeliveryPersonJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<DeliveryPerson> findAll() {
        return springDataRepository.findAll().stream()
                .map(DeliveryPersonJpaEntity::toDomain)
                .toList();
    }

    @Override
    public DeliveryPerson save(DeliveryPerson deliveryPerson) {
        DeliveryPersonJpaEntity entity = DeliveryPersonJpaEntity.fromDomain(deliveryPerson);
        DeliveryPersonJpaEntity saved = springDataRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void delete(DeliveryPerson deliveryPerson) {
        springDataRepository.deleteById(deliveryPerson.getId().value());
    }

    @Override
    public boolean existsById(DeliveryPersonId id) {
        return springDataRepository.existsById(id.value());
    }
}