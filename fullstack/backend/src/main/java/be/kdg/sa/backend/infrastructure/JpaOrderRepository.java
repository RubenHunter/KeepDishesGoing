package be.kdg.sa.backend.infrastructure;
import be.kdg.sa.backend.domain.Entities.Order;
import be.kdg.sa.backend.domain.OrderRepository;
import be.kdg.sa.backend.domain.ValueObjects.OrderId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaOrderRepository implements OrderRepository {
    private final SpringDataOrderRepository springDataRepository;

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return springDataRepository.findById(orderId.getValue())
                .map(OrderJpaEntity::toDomain);
    }

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = OrderJpaEntity.fromDomain(order);
        OrderJpaEntity saved = springDataRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void delete(Order order) {
        springDataRepository.deleteById(order.getId().getValue());
    }

    @Override
    public boolean existsById(OrderId orderId) {
        return springDataRepository.existsById(orderId.getValue());
    }
}