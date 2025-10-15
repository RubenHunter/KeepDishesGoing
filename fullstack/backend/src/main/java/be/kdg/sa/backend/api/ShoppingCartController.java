package be.kdg.sa.backend.api;

import be.kdg.sa.backend.application.ShoppingCartApplicationService;
import be.kdg.sa.backend.domain.Order.CustomerId;
import be.kdg.sa.backend.domain.Order.MenuItemId;
import be.kdg.sa.backend.domain.Order.RestaurantId;
import be.kdg.sa.backend.domain.Shared.Money;
import be.kdg.sa.backend.domain.Shared.Quantity;
import be.kdg.sa.backend.domain.Shared.ShoppingCart;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class ShoppingCartController {
    private final ShoppingCartApplicationService shoppingCartService;

    @GetMapping("/{customerId}")
    public ResponseEntity<ShoppingCartResponse> getCart(@PathVariable String customerId) {
        ShoppingCart cart = shoppingCartService.getCart(CustomerId.of(customerId));
        return ResponseEntity.ok(ShoppingCartResponse.fromDomain(cart));
    }

    @PostMapping("/{customerId}/items")
    public ResponseEntity<Void> addItemToCart(
            @PathVariable String customerId,
            @Valid @RequestBody AddItemRequest request) {

        shoppingCartService.addItemToCart(
                CustomerId.of(customerId),
                MenuItemId.of(request.menuItemId()),
                request.itemName(),
                Quantity.of(request.quantity()),
                Money.ofEuros(request.unitPrice()),
                RestaurantId.of(request.restaurantId())
        );

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{customerId}/items/{menuItemId}")
    public ResponseEntity<Void> removeItemFromCart(
            @PathVariable String customerId,
            @PathVariable String menuItemId) {

        shoppingCartService.removeItemFromCart(
                CustomerId.of(customerId),
                MenuItemId.of(menuItemId)
        );

        return ResponseEntity.ok().build();
    }

    @PutMapping("/{customerId}/items/{menuItemId}")
    public ResponseEntity<Void> updateItemQuantity(
            @PathVariable String customerId,
            @PathVariable String menuItemId,
            @Valid @RequestBody UpdateQuantityRequest request) {

        shoppingCartService.updateItemQuantity(
                CustomerId.of(customerId),
                MenuItemId.of(menuItemId),
                Quantity.of(request.quantity())
        );

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> clearCart(@PathVariable String customerId) {
        shoppingCartService.clearCart(CustomerId.of(customerId));
        return ResponseEntity.ok().build();
    }

    public record AddItemRequest(
            String menuItemId,
            String itemName,
            int quantity,
            double unitPrice,
            String restaurantId
    ) {}

    public record UpdateQuantityRequest(int quantity) {}

    public record ShoppingCartResponse(
            String cartId,
            String customerId,
            String restaurantId,
            java.util.List<CartItemResponse> items,
            double totalAmount,
            String currency
    ) {
        public static ShoppingCartResponse fromDomain(ShoppingCart cart) {
            return new ShoppingCartResponse(
                    cart.getId().getValue(),
                    cart.getCustomerId().getValue(),
                    cart.getRestaurantId() != null ? cart.getRestaurantId().getValue() : null,
                    cart.getItems().stream()
                            .map(CartItemResponse::fromDomain)
                            .toList(),
                    cart.getTotalAmount().getAmount().doubleValue(),
                    cart.getTotalAmount().getCurrency()
            );
        }
    }

    public record CartItemResponse(
            String menuItemId,
            String itemName,
            int quantity,
            double unitPrice,
            double lineTotal,
            String currency
    ) {
        public static CartItemResponse fromDomain(be.kdg.sa.backend.domain.Shared.CartItem item) {
            return new CartItemResponse(
                    item.getMenuItemId().getValue(),
                    item.getItemName(),
                    item.getQuantity().getValue(),
                    item.getUnitPrice().getAmount().doubleValue(),
                    item.calculateLineTotal().getAmount().doubleValue(),
                    item.getUnitPrice().getCurrency()
            );
        }
    }
}