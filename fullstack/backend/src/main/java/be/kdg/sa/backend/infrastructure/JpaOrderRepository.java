package be.kdg.sa.backend.infrastructure;

import be.kdg.sa.backend.domain.Order.Order;
import be.kdg.sa.backend.domain.Order.OrderId;
import be.kdg.sa.backend.domain.Order.OrderItem;
import be.kdg.sa.backend.domain.OrderRepository;
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

        for (OrderItem domainItem : order.getItems()) {
            OrderItemJpaEntity itemEntity = OrderItemJpaEntity.fromDomain(domainItem, entity);
            entity.addOrderItem(itemEntity);
        }

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