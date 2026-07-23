package be.kdg.backend.infrastructure.persistence.tracking;

import be.kdg.backend.application.tracking.OrderEventEntry;
import be.kdg.backend.application.tracking.OrderEventHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DbOrderEventHistoryRepository implements OrderEventHistoryRepository {

    private final SpringDataOrderEventJpaRepository spring;

    @Override
    public OrderEventEntry save(OrderEventEntry entry) {
        return spring.save(JpaOrderEventEntity.from(entry)).toDomain();
    }

    @Override
    public List<OrderEventEntry> findByOrderId(UUID orderId) {
        return spring.findByOrderIdOrderByOccurredAt(orderId).stream()
                .map(JpaOrderEventEntity::toDomain)
                .toList();
    }
}