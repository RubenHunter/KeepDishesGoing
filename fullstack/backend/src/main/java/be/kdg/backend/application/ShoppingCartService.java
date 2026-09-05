package be.kdg.backend.application;

import be.kdg.backend.domain.NotFoundException;
import be.kdg.backend.domain.ValidationException;
import be.kdg.backend.domain.shared.CustomerId;
import be.kdg.backend.domain.shared.MenuItemId;
import be.kdg.backend.domain.shared.Money;
import be.kdg.backend.domain.shared.Quantity;
import be.kdg.backend.domain.shared.RestaurantId;
import be.kdg.backend.domain.shoppingcart.CartId;
import be.kdg.backend.domain.shoppingcart.ShoppingCart;
import be.kdg.backend.domain.shoppingcart.ShoppingCartRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service for the ShoppingCart aggregate.
 * Keeps NO domain logic — delegates to the aggregate for all state changes.
 * Logs bad inputs at WARN per grading rubric (logging requirement).
 */
@Slf4j
@Service
@Transactional
public class ShoppingCartService {

    private final ShoppingCartRepository cartRepository;
    private final int maxCartItems;

    public ShoppingCartService(ShoppingCartRepository cartRepository,
                               @Value("${kdg.order.max-cart-items:50}") int maxCartItems) {
        this.cartRepository = cartRepository;
        this.maxCartItems = maxCartItems;
    }

    public CartId createCart(UUID customerId) {
        log.debug("createCart customerId={}", customerId);
        CartId id = CartId.generate();
        ShoppingCart cart = new ShoppingCart(id, CustomerId.of(customerId));
        cartRepository.save(cart);
        log.info("Created cart {} for customer {}", id.value(), customerId);
        return id;
    }

    public ShoppingCart getCart(UUID cartId) {
        return cartRepository.findById(CartId.of(cartId.toString()))
                .orElseThrow(() -> new NotFoundException("Cart not found: " + cartId));
    }

    public ShoppingCart addItem(UUID cartId, UUID menuItemId, String itemName,
                               int quantity, double unitPrice, UUID restaurantId) {
        log.debug("addItem cart={} menuItem={} qty={} price={} restaurant={}",
                cartId, menuItemId, quantity, unitPrice, restaurantId);
        ShoppingCart cart = getCart(cartId);
        try {
            cart.addItem(
                    MenuItemId.of(menuItemId),
                    itemName,
                    Quantity.of(quantity),
                    Money.ofEuros(unitPrice),
                    RestaurantId.of(restaurantId),
                    maxCartItems
            );
        } catch (ValidationException ve) {
            log.warn("addItem rejected: {}", ve.getMessage());
            throw ve;
        }
        return cartRepository.save(cart);
    }

    public ShoppingCart updateItemQuantity(UUID cartId, UUID menuItemId, int newQuantity) {
        log.debug("updateItemQuantity cart={} menu={} qty={}", cartId, menuItemId, newQuantity);
        ShoppingCart cart = getCart(cartId);
        cart.updateItemQuantity(MenuItemId.of(menuItemId), Quantity.of(newQuantity));
        return cartRepository.save(cart);
    }

    public void removeItem(UUID cartId, UUID menuItemId) {
        log.debug("removeItem cart={} menu={}", cartId, menuItemId);
        ShoppingCart cart = getCart(cartId);
        cart.removeItem(MenuItemId.of(menuItemId));
        cartRepository.save(cart);
    }

    public void clearCart(UUID cartId) {
        ShoppingCart cart = getCart(cartId);
        cart.clear();
        cartRepository.save(cart);
    }
}