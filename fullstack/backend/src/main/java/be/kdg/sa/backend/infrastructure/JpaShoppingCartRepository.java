package be.kdg.sa.backend.infrastructure;

import be.kdg.sa.backend.domain.Order.CustomerId;
import be.kdg.sa.backend.domain.Shared.CartItem;
import be.kdg.sa.backend.domain.Shared.ShoppingCart;
import be.kdg.sa.backend.domain.Shared.ShoppingCartId;
import be.kdg.sa.backend.domain.ShoppingCartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaShoppingCartRepository implements ShoppingCartRepository {
    private final SpringDataShoppingCartRepository springDataRepository;

    @Override
    public Optional<ShoppingCart> findById(ShoppingCartId id) {
        return springDataRepository.findById(id.getValue())
                .map(ShoppingCartJpaEntity::toDomain);
    }

    @Override
    public Optional<ShoppingCart> findByCustomerId(CustomerId customerId) {
        ShoppingCartJpaEntity entity = springDataRepository.findByCustomerId(customerId.getValue());
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public ShoppingCart save(ShoppingCart shoppingCart) {
        ShoppingCartJpaEntity entity = ShoppingCartJpaEntity.fromDomain(shoppingCart);

        for (CartItem domainItem : shoppingCart.getItems()) {
            CartItemJpaEntity itemEntity = CartItemJpaEntity.fromDomain(domainItem, entity);
            entity.addCartItem(itemEntity);
        }

        ShoppingCartJpaEntity saved = springDataRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void delete(ShoppingCart shoppingCart) {
        springDataRepository.deleteById(shoppingCart.getId().getValue());
    }

    @Override
    public boolean existsById(ShoppingCartId id) {
        return springDataRepository.existsById(id.getValue());
    }
}