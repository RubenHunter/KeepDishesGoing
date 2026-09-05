package be.kdg.backend.domain.order;

import be.kdg.backend.domain.ValidationException;
import be.kdg.backend.domain.shared.CustomerId;
import be.kdg.backend.domain.shared.MenuItemId;
import be.kdg.backend.domain.shared.Money;
import be.kdg.backend.domain.shared.OrderSnapshot;
import be.kdg.backend.domain.shared.Quantity;
import be.kdg.backend.domain.shared.RestaurantId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    private OrderSnapshot sampleSnapshot(UUID restId) {
        return new OrderSnapshot(
                RestaurantId.of(restId),
                List.of(new OrderSnapshot.SnapshotItem(
                        MenuItemId.of(UUID.randomUUID()),
                        "Pizza",
                        Quantity.of(2),
                        Money.ofEuros(10)
                )),
                Money.ofEuros(20)
        );
    }

    private Order newOrder(UUID restId) {
        return new Order(
                OrderId.generate(),
                CustomerId.generate(),
                RestaurantId.of(restId),
                "Ruben",
                new be.kdg.backend.domain.shared.Address("S", "1", "2000", "A", "BE"),
                new be.kdg.backend.domain.shared.Email("r@example.com"),
                sampleSnapshot(restId)
        );
    }

    @Test
    void newOrderIsPending() {
        Order o = newOrder(UUID.randomUUID());
        assertEquals(OrderStatus.PENDING, o.status());
        assertFalse(o.isPlaced());
        assertTrue(o.isModifiable());
        assertEquals(PaymentStatus.AWAITING, o.paymentStatus());
    }

    @Test
    void placeRequiresPayment() {
        Order o = newOrder(UUID.randomUUID());
        IllegalStateException ex = assertThrows(IllegalStateException.class, o::place);
        assertTrue(ex.getMessage().contains("paid"));
    }

    @Test
    void placeTransitionsToPlacedAndFreezes() {
        Order o = newOrder(UUID.randomUUID());
        o.assignPayment("pay-1", PaymentStatus.PAID);
        o.place();
        assertEquals(OrderStatus.PLACED, o.status());
        assertNotNull(o.placedAt());
        assertTrue(o.isPlaced());
        assertFalse(o.isModifiable());
    }

    @Test
    void acceptOnlyFromPlaced() {
        Order o = newOrder(UUID.randomUUID());
        assertThrows(OrderFrozenException.class, o::accept); // PENDING

        o.assignPayment("p", PaymentStatus.PAID);
        o.place();
        o.accept();
        assertEquals(OrderStatus.ACCEPTED, o.status());
        assertNotNull(o.acceptedAt());
    }

    @Test
    void rejectRequiresReason() {
        Order o = newOrder(UUID.randomUUID());
        o.assignPayment("p", PaymentStatus.PAID);
        o.place();
        assertThrows(IllegalArgumentException.class, () -> o.reject(""));
        o.reject("kitchen closed");
        assertEquals(OrderStatus.REJECTED, o.status());
        assertEquals("kitchen closed", o.rejectReason());
        assertTrue(o.status().isTerminal());
    }

    @Test
    void fullHappyPath() {
        Order o = newOrder(UUID.randomUUID());
        o.assignPayment("p", PaymentStatus.PAID);
        o.place();
        o.accept();
        o.markReadyForPickup();
        o.markPickedUp();
        o.markDelivered();
        assertEquals(OrderStatus.DELIVERED, o.status());
        assertTrue(o.status().isTerminal());
        assertNotNull(o.placedAt());
        assertNotNull(o.acceptedAt());
        assertNotNull(o.readyAt());
        assertNotNull(o.pickedUpAt());
        assertNotNull(o.deliveredAt());
    }

    @Test
    void cancelFromPlaced() {
        Order o = newOrder(UUID.randomUUID());
        o.assignPayment("p", PaymentStatus.PAID);
        o.place();
        o.cancel("reason");
        assertEquals(OrderStatus.CANCELLED, o.status());
    }

    @Test
    void cancelFromPending() {
        Order o = newOrder(UUID.randomUUID());
        o.cancel("abandoned during testing");
        assertEquals(OrderStatus.CANCELLED, o.status());
        assertEquals("abandoned during testing", o.rejectReason());
    }

    @Test
    void cancelFromAcceptedAllowed() {
        Order o = newOrder(UUID.randomUUID());
        o.assignPayment("p", PaymentStatus.PAID);
        o.place();
        o.accept();
        o.cancel("reason");
        assertEquals(OrderStatus.CANCELLED, o.status());
    }

    @Test
    void cannotCancelFromDelivered() {
        Order o = newOrder(UUID.randomUUID());
        o.assignPayment("p", PaymentStatus.PAID);
        o.place();
        o.accept();
        o.markReadyForPickup();
        o.markPickedUp();
        o.markDelivered();
        assertThrows(OrderFrozenException.class, () -> o.cancel("oops"));
    }

    @Test
    void pickingUpBeforeReadyThrows() {
        Order o = newOrder(UUID.randomUUID());
        o.assignPayment("p", PaymentStatus.PAID);
        o.place();
        o.accept();
        assertThrows(OrderFrozenException.class, o::markPickedUp);
    }

    @Test
    void rehydrateValidatesConsistency() {
        Order o = newOrder(UUID.randomUUID());
        // Simulate bad data: ACCEPTED but no acceptedAt
        assertThrows(IllegalStateException.class, () -> Order.rehydrate(
                o.id(), o.customerId(), o.restaurantId(), o.items(), o.totalAmount(),
                o.customerName(), o.deliveryAddress(), o.customerEmail(),
                OrderStatus.ACCEPTED, null, null, PaymentStatus.AWAITING,
                o.createdAt(), null, null, null, null, null, null, null, null
        ));
    }

    @Test
    void emptySnapshotThrows() {
        UUID rest = UUID.randomUUID();
        OrderSnapshot empty = new OrderSnapshot(RestaurantId.of(rest), List.of(), Money.ZERO);
        assertThrows(IllegalArgumentException.class, () -> new Order(
                OrderId.generate(), CustomerId.generate(), RestaurantId.of(rest),
                "Ruben",
                new be.kdg.backend.domain.shared.Address("S", "1", "2000", "A", "BE"),
                new be.kdg.backend.domain.shared.Email("r@example.com"),
                empty
        ));
    }

    @Test
    void restaurantIdMismatchThrows() {
        UUID restA = UUID.randomUUID();
        UUID restB = UUID.randomUUID();
        OrderSnapshot snapWithRestB = new OrderSnapshot(
                RestaurantId.of(restB),
                List.of(new OrderSnapshot.SnapshotItem(
                        MenuItemId.of(UUID.randomUUID()), "Pizza", Quantity.of(2), Money.ofEuros(10))),
                Money.ofEuros(20)
        );
        assertThrows(IllegalArgumentException.class, () -> new Order(
                OrderId.generate(), CustomerId.generate(), RestaurantId.of(restA),
                "Ruben",
                new be.kdg.backend.domain.shared.Address("S", "1", "2000", "A", "BE"),
                new be.kdg.backend.domain.shared.Email("r@example.com"),
                snapWithRestB
        ));
    }
}