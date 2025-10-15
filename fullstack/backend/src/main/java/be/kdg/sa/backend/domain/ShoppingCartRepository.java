package be.kdg.sa.backend.domain;
import be.kdg.sa.backend.domain.Order.CustomerId;
import be.kdg.sa.backend.domain.Shared.ShoppingCart;
import be.kdg.sa.backend.domain.Shared.ShoppingCartId;
import org.jmolecules.ddd.annotation.Repository;

import java.util.Optional;

@Repository
public interface ShoppingCartRepository {
    Optional<ShoppingCart> findById(ShoppingCartId id);
    Optional<ShoppingCart> findByCustomerId(CustomerId customerId);
    ShoppingCart save(ShoppingCart shoppingCart);
    void delete(ShoppingCart shoppingCart);
    boolean existsById(ShoppingCartId id);
}