package be.kdg.backend.domain.order;

import be.kdg.backend.domain.shared.CustomerId;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for {@link Order} aggregate root. Owned by domain; implemented in infrastructure.
 */
public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(OrderId id);
    List<Order> findByCustomerId(CustomerId customerId);
    Optional<Order> findByPaymentRef(String paymentRef);
}