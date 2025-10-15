package be.kdg.sa.backend.USTEST;

import be.kdg.sa.backend.application.ShoppingCartApplicationService;
import be.kdg.sa.backend.domain.Order.CustomerId;
import be.kdg.sa.backend.domain.Order.MenuItemId;
import be.kdg.sa.backend.domain.Order.RestaurantId;
import be.kdg.sa.backend.domain.Shared.Money;
import be.kdg.sa.backend.domain.Shared.Quantity;
import be.kdg.sa.backend.domain.Shared.ShoppingCart;
import be.kdg.sa.backend.domain.Shared.ShoppingCartRestaurantException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("ShoppingCartApplicationService - US16 Integration")
class ShoppingCartApplicationServiceUS16Test {

    @Autowired
    private ShoppingCartApplicationService shoppingCartService;

    private CustomerId customerId;
    private RestaurantId restaurantPizza;
    private RestaurantId restaurantSushi;

    @BeforeEach
    void setUp() {
        customerId = CustomerId.generate();
        restaurantPizza = RestaurantId.of("REST-PIZZA");
        restaurantSushi = RestaurantId.of("REST-SUSHI");
    }
//ng
    @Test
    @DisplayName("Should enforce single restaurant per cart in application service")
    void shouldEnforceSingleRestaurantPerCartInApplicationService() {
        // Given
        shoppingCartService.addItemToCart(
                customerId,
                MenuItemId.of("MENU-PIZZA"),
                "Margherita Pizza",
                Quantity.of(1),
                Money.ofEuros(12.50),
                restaurantPizza
        );

        // When & Then
        ShoppingCartRestaurantException exception = assertThrows(ShoppingCartRestaurantException.class, () -> {
            shoppingCartService.addItemToCart(
                    customerId,
                    MenuItemId.of("MENU-SUSHI"),
                    "California Roll",
                    Quantity.of(1),
                    Money.ofEuros(15.00),
                    restaurantSushi
            );
        });

        ShoppingCart cart = shoppingCartService.getCart(customerId);
        assertEquals(1, cart.getItemCount());
        assertEquals(restaurantPizza, cart.getRestaurantId());
    }
//ng
    @Test
    @DisplayName("Should persist and retrieve cart with restaurant constraint")
    void shouldPersistAndRetrieveCartWithRestaurantConstraint() {
        // Given
        shoppingCartService.addItemToCart(
                customerId,
                MenuItemId.of("MENU-PIZZA"),
                "Margherita Pizza",
                Quantity.of(2),
                Money.ofEuros(12.50),
                restaurantPizza
        );

        shoppingCartService.addItemToCart(
                customerId,
                MenuItemId.of("MENU-PASTA"),
                "Spaghetti Carbonara",
                Quantity.of(1),
                Money.ofEuros(8.75),
                restaurantPizza
        );

        // When
        ShoppingCart retrievedCart = shoppingCartService.getCart(customerId);

        // Then
        assertEquals(2, retrievedCart.getItemCount());
        assertEquals(restaurantPizza, retrievedCart.getRestaurantId());
        assertEquals(Money.ofEuros(33.75), retrievedCart.getTotalAmount()); // (2 * 12.50) + 8.75
        assertTrue(retrievedCart.containsItemsFromRestaurant(restaurantPizza));
    }
}