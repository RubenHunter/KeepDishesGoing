package be.kdg.sa.backend.application;

import be.kdg.sa.backend.domain.Order.CustomerId;
import be.kdg.sa.backend.domain.Order.MenuItemId;
import be.kdg.sa.backend.domain.Order.RestaurantId;
import be.kdg.sa.backend.domain.Shared.Money;
import be.kdg.sa.backend.domain.Shared.Quantity;
import be.kdg.sa.backend.domain.Shared.ShoppingCart;
import be.kdg.sa.backend.domain.Shared.ShoppingCartId;
import be.kdg.sa.backend.domain.ShoppingCartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ShoppingCartApplicationService {
    private final ShoppingCartRepository shoppingCartRepository;

    public ShoppingCartId getOrCreateCartForCustomer(CustomerId customerId) {
        return shoppingCartRepository.findByCustomerId(customerId)
                .map(ShoppingCart::getId)
                .orElseGet(() -> createNewCart(customerId));
    }

    public ShoppingCart getCart(CustomerId customerId) {
        return shoppingCartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ShoppingCartNotFoundException("Shopping cart not found for customer: " + customerId.getValue()));
    }

    public void addItemToCart(CustomerId customerId, MenuItemId menuItemId, String itemName,
                              Quantity quantity, Money unitPrice, RestaurantId restaurantId) {
        ShoppingCart cart = getOrCreateCart(customerId);

        cart.addItem(menuItemId, itemName, quantity, unitPrice, restaurantId);
        shoppingCartRepository.save(cart);

        log.info("Added item to cart: customer={}, menuItem={}, quantity={}",
                customerId.getValue(), menuItemId.getValue(), quantity.getValue());
    }

    public void removeItemFromCart(CustomerId customerId, MenuItemId menuItemId) {
        ShoppingCart cart = getCart(customerId);
        cart.removeItem(menuItemId);
        shoppingCartRepository.save(cart);

        log.info("Removed item from cart: customer={}, menuItem={}",
                customerId.getValue(), menuItemId.getValue());
    }

    public void updateItemQuantity(CustomerId customerId, MenuItemId menuItemId, Quantity quantity) {
        ShoppingCart cart = getCart(customerId);
        cart.updateItemQuantity(menuItemId, quantity);
        shoppingCartRepository.save(cart);

        log.info("Updated item quantity in cart: customer={}, menuItem={}, quantity={}",
                customerId.getValue(), menuItemId.getValue(), quantity.getValue());
    }

    public void clearCart(CustomerId customerId) {
        ShoppingCart cart = getCart(customerId);
        cart.clearCart();
        shoppingCartRepository.save(cart);

        log.info("Cleared cart for customer: {}", customerId.getValue());
    }

    private ShoppingCart getOrCreateCart(CustomerId customerId) {
        return shoppingCartRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    ShoppingCart newCart = new ShoppingCart(ShoppingCartId.generate(), customerId);
                    return shoppingCartRepository.save(newCart);
                });
    }

    private ShoppingCartId createNewCart(CustomerId customerId) {
        ShoppingCart newCart = new ShoppingCart(ShoppingCartId.generate(), customerId);
        ShoppingCart savedCart = shoppingCartRepository.save(newCart);
        return savedCart.getId();
    }
}