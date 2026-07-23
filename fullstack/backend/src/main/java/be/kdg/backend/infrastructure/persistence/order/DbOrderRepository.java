package be.kdg.backend.infrastructure.persistence.order;

import be.kdg.backend.domain.order.Order;
import be.kdg.backend.domain.order.OrderId;
import be.kdg.backend.domain.order.OrderRepository;
import be.kdg.backend.domain.shared.CustomerId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Infrastructure-side adapter implementing the domain {@link OrderRepository} port. */
@Repository
@RequiredArgsConstructor
public class DbOrderRepository implements OrderRepository {

    private final SpringDataOrderJpaRepository spring;

    @Override
    public Order save(Order order) {
        JpaOrderEntity saved = spring.save(JpaOrderEntity.fromDomain(order));
        return saved.toDomain();
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return spring.findById(id.value()).map(JpaOrderEntity::toDomain);
    }

    @Override
    public List<Order> findByCustomerId(CustomerId customerId) {
        return spring.findByCustomerId(customerId.value()).stream()
                .map(JpaOrderEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Order> findByPaymentRef(String paymentRef) {
        return spring.findByPaymentRef(paymentRef).map(JpaOrderEntity::toDomain);
    }

    UUID asUuid(Object id) { return (UUID) id; }
}