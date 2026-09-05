package be.kdg.backend.domain.shoppingcart;

import be.kdg.backend.domain.shoppingcart.ShoppingCart;
import java.util.Optional;

/**
 * Repository port for {@link ShoppingCart} aggregate root. Owned by domain.
 * Implemented in infrastructure layer with JPA.
 */
public interface ShoppingCartRepository {
    ShoppingCart save(ShoppingCart cart);
    Optional<ShoppingCart> findById(CartId id);
    void deleteById(CartId id);
}