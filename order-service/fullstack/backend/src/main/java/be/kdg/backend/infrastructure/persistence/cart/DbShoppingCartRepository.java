package be.kdg.backend.infrastructure.persistence.cart;

import be.kdg.backend.domain.shoppingcart.CartId;
import be.kdg.backend.domain.shoppingcart.ShoppingCart;
import be.kdg.backend.domain.shoppingcart.ShoppingCartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DbShoppingCartRepository implements ShoppingCartRepository {

    private final SpringDataCartJpaRepository spring;

    @Override
    public ShoppingCart save(ShoppingCart cart) {
        return spring.save(JpaCartEntity.fromDomain(cart)).toDomain();
    }

    @Override
    public Optional<ShoppingCart> findById(CartId id) {
        return spring.findById(id.value()).map(JpaCartEntity::toDomain);
    }

    @Override
    public void deleteById(CartId id) {
        spring.deleteById(id.value());
    }
}