package be.kdg.backend.application;

import be.kdg.backend.application.messaging.EventPublisher;
import be.kdg.backend.application.payment.PaymentGateway;
import be.kdg.backend.application.restaurant.RestaurantGateway;
import be.kdg.backend.domain.NotFoundException;
import be.kdg.backend.domain.order.Order;
import be.kdg.backend.domain.order.OrderId;
import be.kdg.backend.domain.order.OrderRepository;
import be.kdg.backend.domain.order.PaymentStatus;
import be.kdg.backend.domain.order.RestaurantClosedException;
import be.kdg.backend.domain.shared.Address;
import be.kdg.backend.domain.shared.CustomerId;
import be.kdg.backend.domain.shared.Email;
import be.kdg.backend.domain.shared.OrderSnapshot;
import be.kdg.backend.domain.shared.RestaurantId;
import be.kdg.backend.domain.shoppingcart.CartId;
import be.kdg.backend.domain.shoppingcart.ShoppingCart;
import be.kdg.backend.domain.shoppingcart.ShoppingCartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Application service for the {@link Order} aggregate.
 * NO domain logic — only coordinates cart, restaurant validation, payment, persistence, and event publishing.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ShoppingCartRepository cartRepository;
    private final RestaurantGateway restaurantGateway;
    private final PaymentGateway paymentGateway;
    private final EventPublisher eventPublisher;

    /**
     * US15/US17/US19/US20 + US18 freeze.
     * Steps:
     *  1. Load cart.
     *  2. Validate every item against restaurant-service (US17).
     *  3. Build frozen OrderSnapshot (US18).
     *  4. Create PENDING Order.
     *  5. Start payment (US20); assign paymentRef to order.
     *  6. Return orderId + paymentRedirectUrl.
     */
    public CheckoutResult checkout(UUID cartId,
                                   UUID customerId,
                                   String customerName,
                                   String street, String number, String postal,
                                   String city, String country,
                                   String email) {
        log.info("checkout cart={} customer={}", cartId, customerId);

        ShoppingCart cart = cartRepository.findById(CartId.of(cartId.toString()))
                .orElseThrow(() -> new NotFoundException("Cart not found: " + cartId));
        if (cart.isEmpty()) {
            log.warn("checkout rejected — empty cart {}", cartId);
            throw new IllegalStateException("Cannot checkout empty cart");
        }

        // US17 validate menu items + prices against restaurant-service
        var validation = restaurantGateway.validateMenuItems(
                new RestaurantGateway.MenuValidationRequest(
                        cart.restaurantId().value(),
                        cart.items().stream()
                                .map(ci -> new RestaurantGateway.MenuValidationRequest.ItemToValidate(
                                        ci.getMenuItemId().value(),
                                        ci.getUnitPrice().amount().doubleValue()))
                                .toList()
                ));
        if (!validation.valid()) {
            log.warn("checkout cart={} rejected by restaurant: {}", cartId, validation.message());
            throw new IllegalStateException("Menu validation failed: " + validation.message());
        }

        OrderSnapshot snapshot = OrderSnapshot.from(cart);
        Order order = new Order(
                OrderId.generate(),
                CustomerId.of(customerId),
                cart.restaurantId(),
                customerName,
                new Address(street, number, postal, city, country),
                new Email(email),
                snapshot
        );

        // US20 start payment
        var paymentRequest = new PaymentGateway.StartPaymentRequest(
                order.id().value().toString(),
                snapshot.total().amount().doubleValue(),
                snapshot.total().currency()
        );
        var payment = paymentGateway.startPayment(paymentRequest);
        order.assignPayment(payment.paymentRef(), PaymentStatus.AWAITING);

        Order saved = orderRepository.save(order);
        log.info("Created PENDING order {} paymentRef={}", saved.id(), saved.paymentRef());

        return new CheckoutResult(
                saved.id().value(),
                saved.status().name(),
                payment.paymentRef(),
                payment.redirectUrl()
        );
    }

    /** Called by payment webhook (or dev stub endpoint). Idempotent — webhook redelivery is a no-op. */
    public void confirmPayment(String paymentRef) {
        log.info("confirmPayment paymentRef={}", paymentRef);
        Order order = findByPaymentRef(paymentRef);
        if (order.isPaid() || order.isPlaced()) {
            log.info("Order {} already paid/placed — ignoring webhook redelivery", order.id());
            return;
        }
        var confirmation = paymentGateway.confirm(paymentRef);
        order.assignPayment(
                paymentRef,
                confirmation.status() == PaymentGateway.PaymentConfirmation.PaymentStatus.PAID
                        ? PaymentStatus.PAID : PaymentStatus.FAILED
        );
        orderRepository.save(order);
        log.info("Order {} payment status updated to {}", order.id(), order.paymentStatus());
    }

    /** US18 place — only valid after payment PAID. Triggers the OrderPlaced event. */
    public void placeOrder(UUID orderId) {
        log.info("placeOrder {}", orderId);
        Order order = loadOrder(orderId);
        ensureRestaurantOpen(order.restaurantId().value());
        try {
            order.place();
        } catch (RuntimeException e) {
            log.warn("placeOrder {} rejected: {}", orderId, e.getMessage());
            throw e;
        }
        orderRepository.save(order);

        var event = new EventPublisher.OrderPlacedEvent(
                order.id().value().toString(),
                order.customerId().value().toString(),
                order.restaurantId().value().toString(),
                order.deliveryAddress().singleLine(),
                order.items().stream()
                        .map(it -> new EventPublisher.OrderPlacedEvent.OrderPlacedItem(
                                it.getMenuItemId().value().toString(),
                                it.getItemName(),
                                it.getQuantity().value(),
                                it.getUnitPrice().amount().doubleValue()))
                        .toList(),
                order.placedAt()
        );
        eventPublisher.publishOrderPlaced(event);
        log.info("Order {} PLACED — published OrderPlaced", orderId);
    }

    public void cancelOrder(UUID orderId, String reason) {
        log.info("cancelOrder {} reason={}", orderId, reason);
        Order order = loadOrder(orderId);
        order.cancel(reason);
        orderRepository.save(order);
        var event = new EventPublisher.OrderCancelledEvent(
                order.id().value().toString(), reason, order.cancelledAt());
        eventPublisher.publishOrderCancelled(event);
        log.info("Order {} CANCELLED — published OrderCancelled", orderId);
    }

    public Order loadOrder(UUID orderId) {
        return orderRepository.findById(OrderId.of(orderId.toString()))
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
    }

    public List<Order> listOrdersForCustomer(UUID customerId) {
        return orderRepository.findByCustomerId(CustomerId.of(customerId));
    }

    /** Owner console — all orders for a restaurant. */
    public List<Order> listOrdersForRestaurant(UUID restaurantId) {
        return orderRepository.findByRestaurantId(RestaurantId.of(restaurantId.toString()));
    }

    // --- Handler entry points (called by AMQP consumers — not exposed via HTTP) ---

    public void onOrderAccepted(UUID orderId, LocalDateTime acceptedAt) {
        log.info("onOrderAccepted {} at {}", orderId, acceptedAt);
        Order order = loadOrder(orderId);
        order.accept();
        orderRepository.save(order);
    }

    public void onOrderRejected(UUID orderId, String reason, LocalDateTime rejectedAt) {
        log.info("onOrderRejected {} reason={}", orderId, reason);
        Order order = loadOrder(orderId);
        order.reject(reason);
        orderRepository.save(order);
    }

    public void onOrderReadyForPickup(UUID orderId) {
        log.info("onOrderReadyForPickup {}", orderId);
        Order order = loadOrder(orderId);
        order.markReadyForPickup();
        orderRepository.save(order);
    }

    public void onOrderPickedUp(UUID orderId) {
        log.info("onOrderPickedUp {}", orderId);
        Order order = loadOrder(orderId);
        order.markPickedUp();
        orderRepository.save(order);
    }

    public void onOrderDelivered(UUID orderId) {
        log.info("onOrderDelivered {}", orderId);
        Order order = loadOrder(orderId);
        order.markDelivered();
        orderRepository.save(order);
    }

    private Order findByPaymentRef(String paymentRef) {
        return orderRepository.findByPaymentRef(paymentRef)
                .orElseThrow(() -> new NotFoundException("Order with paymentRef " + paymentRef + " not found"));
    }

    /** US11 — reject placement while the restaurant is closed (status computed server-side). */
    private void ensureRestaurantOpen(UUID restaurantId) {
        RestaurantGateway.RestaurantStatusDto status = restaurantGateway.getStatus(restaurantId);
        if (!status.openNow()) {
            String when = status.nextOpening() != null
                    ? ", opens " + status.nextOpening()
                    : "";
            throw new RestaurantClosedException("Restaurant closed" + when);
        }
    }

    /** Checkout result DTO returned by API. */
    public record CheckoutResult(UUID orderId, String status, String paymentRef, String redirectUrl) {}
}