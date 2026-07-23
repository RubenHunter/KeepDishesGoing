package be.kdg.backend.api;

import be.kdg.backend.api.dto.AddCartItemRequest;
import be.kdg.backend.api.dto.CartResponse;
import be.kdg.backend.api.dto.CreateCartRequest;
import be.kdg.backend.application.ShoppingCartService;
import be.kdg.backend.domain.shoppingcart.CartId;
import be.kdg.backend.domain.shoppingcart.ShoppingCart;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * Shopping cart endpoints — public per PDF (customer-facing, no auth on order-service).
 */
@Slf4j
@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class ShoppingCartController {

    private final ShoppingCartService cartService;

    @PostMapping
    public ResponseEntity<CartResponse> createCart(@Valid @RequestBody CreateCartRequest req) {
        CartId cartId = cartService.createCart(req.customerId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(cartId.value())
                .toUri();
        ShoppingCart cart = cartService.getCart(cartId.value());
        return ResponseEntity.created(location).body(toDto(cart));
    }

    @GetMapping("/{cartId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable UUID cartId) {
        return ResponseEntity.ok(toDto(cartService.getCart(cartId)));
    }

    @PostMapping("/{cartId}/items")
    public ResponseEntity<CartResponse> addItem(@PathVariable UUID cartId,
                                                @Valid @RequestBody AddCartItemRequest req) {
        ShoppingCart cart = cartService.addItem(
                cartId, req.menuItemId(), req.itemName(),
                req.quantity(), req.unitPrice(), req.restaurantId());
        return ResponseEntity.ok(toDto(cart));
    }

    @PatchMapping("/{cartId}/items/{menuItemId}")
    public ResponseEntity<CartResponse> updateItem(@PathVariable UUID cartId,
                                                    @PathVariable UUID menuItemId,
                                                    @RequestBody UpdateItemRequest req) {
        ShoppingCart cart = cartService.updateItemQuantity(cartId, menuItemId, req.quantity());
        return ResponseEntity.ok(toDto(cart));
    }

    @DeleteMapping("/{cartId}/items/{menuItemId}")
    public ResponseEntity<Void> removeItem(@PathVariable UUID cartId, @PathVariable UUID menuItemId) {
        cartService.removeItem(cartId, menuItemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{cartId}")
    public ResponseEntity<Void> clearCart(@PathVariable UUID cartId) {
        cartService.clearCart(cartId);
        return ResponseEntity.noContent().build();
    }

    private CartResponse toDto(ShoppingCart cart) {
        return new CartResponse(
                cart.id().value(),
                cart.customerId().value(),
                cart.restaurantId() == null ? null : cart.restaurantId().value(),
                cart.items().stream().map(it -> new CartResponse.CartItemResponse(
                        it.getMenuItemId().value(),
                        it.getItemName(),
                        it.getQuantity().value(),
                        it.getUnitPrice().amount().doubleValue(),
                        it.calculateLineTotal().amount().doubleValue())).toList(),
                cart.total().amount().doubleValue(),
                cart.total().currency()
        );
    }

    public record UpdateItemRequest(int quantity) {}
}