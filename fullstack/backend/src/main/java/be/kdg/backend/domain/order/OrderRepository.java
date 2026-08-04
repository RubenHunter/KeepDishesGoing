package be.kdg.backend.domain.order;

import be.kdg.backend.domain.shared.CustomerId;
import be.kdg.backend.domain.shared.RestaurantId;

import java.time.LocalDateTime;
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

    /** US23/US24 — orders still PLACED with placedAt before the given cutoff. */
    List<Order> findPlacedBefore(LocalDateTime before);

    /** Owner console — all orders for a restaurant regardless of status. */
    List<Order> findByRestaurantId(RestaurantId restaurantId);
}