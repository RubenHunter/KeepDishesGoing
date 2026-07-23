package be.kdg.backend.infrastructure.persistence.cart;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "carts", schema = "ordering")
public class JpaCartEntity {

    @Id
    private UUID id;

    private UUID customerId;
    private UUID restaurantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "cart_id", nullable = false)
    private List<JpaCartItemEntity> items = new ArrayList<>();

    public JpaCartEntity() {}

    public UUID getId() { return id; }
    public UUID getRestaurantId() { return restaurantId; }
    public List<JpaCartItemEntity> getItems() { return items; }

    public static JpaCartEntity fromDomain(be.kdg.backend.domain.shoppingcart.ShoppingCart cart) {
        JpaCartEntity e = new JpaCartEntity();
        e.id = cart.id().value();
        e.customerId = cart.customerId().value();
        e.restaurantId = cart.restaurantId() == null ? null : cart.restaurantId().value();
        e.createdAt = cart.createdAt();
        e.updatedAt = cart.updatedAt();
        cart.items().forEach(it -> e.items.add(JpaCartItemEntity.from(it, e)));
        return e;
    }

    public be.kdg.backend.domain.shoppingcart.ShoppingCart toDomain() {
        return be.kdg.backend.domain.shoppingcart.ShoppingCart.rehydrate(
                be.kdg.backend.domain.shoppingcart.CartId.of(id.toString()),
                be.kdg.backend.domain.shared.CustomerId.of(customerId),
                restaurantId == null ? null : be.kdg.backend.domain.shared.RestaurantId.of(restaurantId),
                items.stream().map(JpaCartItemEntity::toDomain).toList(),
                createdAt,
                updatedAt
        );
    }
}