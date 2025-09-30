package be.kdg.sa.backend.domain;

import be.kdg.sa.backend.domain.Entities.Order;
import be.kdg.sa.backend.domain.ValueObjects.OrderId;
import org.jmolecules.ddd.annotation.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository {
    Optional<Order> findById(OrderId orderId);
    Order save(Order order);
    void delete(Order order);
    boolean existsById(OrderId orderId);
}

