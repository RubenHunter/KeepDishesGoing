package be.kdg.sa.backend.infrastructure;

// ✅ JPA imports alleen in infrastructure!
import be.kdg.sa.backend.domain.Order.CustomerId;
import be.kdg.sa.backend.domain.Order.RestaurantId;
import be.kdg.sa.backend.domain.Shared.CartItem;
import be.kdg.sa.backend.domain.Shared.Money;
import be.kdg.sa.backend.domain.Shared.ShoppingCart;
import be.kdg.sa.backend.domain.Shared.ShoppingCartId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "shopping_carts", schema = "ordering")
@Getter
@NoArgsConstructor
public class ShoppingCartJpaEntity {
    @Id
    private String id;

    @Column(name = "customer_id", nullable = false, unique = true)
    private String customerId;

    @Column(name = "restaurant_id")
    private String restaurantId;

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @CreationTimestamp
    @Column(name = "create_date", nullable = false)
    private LocalDateTime createDate;

    @UpdateTimestamp
    @Column(name = "update_date", nullable = false)
    private LocalDateTime updateDate;

    @OneToMany(mappedBy = "shoppingCart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CartItemJpaEntity> items = new ArrayList<>();

    // ✅ Alleen infrastructure methods
    public static ShoppingCartJpaEntity fromDomain(ShoppingCart shoppingCart) {
        ShoppingCartJpaEntity entity = new ShoppingCartJpaEntity();
        entity.id = shoppingCart.getId().getValue();
        entity.customerId = shoppingCart.getCustomerId().getValue();
        entity.restaurantId = shoppingCart.getRestaurantId() != null ? shoppingCart.getRestaurantId().getValue() : null;
        entity.totalAmount = shoppingCart.getTotalAmount().getAmount();
        entity.currency = shoppingCart.getTotalAmount().getCurrency();
        entity.createDate = shoppingCart.getCreateDate();
        entity.updateDate = shoppingCart.getUpdateDate();
        return entity;
    }

    public ShoppingCart toDomain() {
        List<CartItem> domainItems = this.items.stream()
                .map(CartItemJpaEntity::toDomain)
                .collect(Collectors.toList());

        return ShoppingCart.reconstruct(
                ShoppingCartId.of(this.id),
                CustomerId.of(this.customerId),
                this.restaurantId != null ? RestaurantId.of(this.restaurantId) : null,
                domainItems,
                Money.of(this.totalAmount, this.currency),
                this.createDate,
                this.updateDate
        );
    }

    // ✅ Package-private method voor interne JPA mapping
    void addCartItem(CartItemJpaEntity cartItem) {
        cartItem.setShoppingCart(this);
        this.items.add(cartItem);
    }
}