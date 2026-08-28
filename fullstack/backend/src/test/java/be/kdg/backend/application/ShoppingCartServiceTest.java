package be.kdg.backend.application;

import be.kdg.backend.application.messaging.EventPublisher;
import be.kdg.backend.application.payment.PaymentGateway;
import be.kdg.backend.application.restaurant.RestaurantGateway;
import be.kdg.backend.domain.NotFoundException;
import be.kdg.backend.domain.ValidationException;
import be.kdg.backend.domain.order.OrderRepository;
import be.kdg.backend.domain.order.OrderStatus;
import be.kdg.backend.domain.shoppingcart.ShoppingCart;
import be.kdg.backend.domain.shoppingcart.ShoppingCartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ShoppingCartService}. All collaborators mocked; no Spring context (US41-style unit rule).
 */
@ExtendWith(MockitoExtension.class)
class ShoppingCartServiceTest {

    @Mock ShoppingCartRepository repo;
    private ShoppingCartService service;

    @BeforeEach
    void setup() {
        service = new ShoppingCartService(repo, 50);
    }

    @Test
    void createCartPersistsNew() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var id = service.createCart(UUID.randomUUID());
        assertNotNull(id);
        verify(repo).save(any(ShoppingCart.class));
    }

    @Test
    void getCartThrowsWhenMissing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.getCart(UUID.randomUUID()));
    }

    @Test
    void addItemDelegatesToAggregate() {
        ShoppingCart cart = mock(ShoppingCart.class);
        when(repo.findById(any())).thenReturn(Optional.of(cart));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.addItem(UUID.randomUUID(), UUID.randomUUID(), "Pizza", 2, 10.0, UUID.randomUUID());
        verify(cart).addItem(any(), eq("Pizza"), any(), any(), any(), eq(50));
        verify(repo).save(cart);
    }
}