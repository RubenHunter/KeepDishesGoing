package be.kdg.sa.backend.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataShoppingCartRepository extends JpaRepository<ShoppingCartJpaEntity, String> {
    ShoppingCartJpaEntity findByCustomerId(String customerId);
}