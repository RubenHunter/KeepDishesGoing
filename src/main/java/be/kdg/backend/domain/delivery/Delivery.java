package be.kdg.backend.domain.delivery;

import be.kdg.backend.domain.shared.Address;
import be.kdg.backend.domain.shared.DeliveryId;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import be.kdg.backend.domain.shared.OrderId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Delivery aggregate root. US27..US30 lifecycle.
 *
 * Status machine:
 *   PENDING  --selfAssign(personId, now)-->        ASSIGNED                       (US27/US28/US32)
 *   ASSIGNED --onOrderReadyForPickup(now)-->        READY_FOR_PICKUP              (US29 boundary lock)
 *   ASSIGNED --cancelClaim(reason, now)-->          CANCELLED                     (US29 only valid here)
 *   READY_FOR_PICKUP --markPickedUp(now)-->         PICKED_UP                     (US30)
 *   PICKED_UP --markInTransit(now)-->                IN_TRANSIT
 *   IN_TRANSIT --markDelivered(now)-->               DELIVERED                     (US30; emits OrderDelivered)
 * No distance/radius rules — US28 says courier simply claims available deliveries.
 */
@AggregateRoot
public class Delivery {

    @Identity
    private final DeliveryId id;
    private final OrderId orderId;
    private final Address pickupAddress;
    private final Address deliveryAddress;

    private DeliveryPersonId deliveryPersonId;
    private DeliveryStatus status;
    private boolean availableForSelfAssignment;

    private LocalDateTime assignedAt;
    private LocalDateTime readyAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime inTransitAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;

    public Delivery(DeliveryId id, OrderId orderId, Address pickupAddress, Address deliveryAddress) {
        requireNonNull(id, "id");
        requireNonNull(orderId, "orderId");
        requireNonNull(pickupAddress, "pickupAddress");
        requireNonNull(deliveryAddress, "deliveryAddress");
        this.id = id;
        this.orderId = orderId;
        this.pickupAddress = pickupAddress;
        this.deliveryAddress = deliveryAddress;
        this.status = DeliveryStatus.PENDING;
        this.availableForSelfAssignment = true;
    }

    private Delivery(DeliveryId id, OrderId orderId, Address pickupAddress, Address deliveryAddress,
                     DeliveryPersonId deliveryPersonId, DeliveryStatus status, boolean availableForSelfAssignment,
                     LocalDateTime assignedAt, LocalDateTime readyAt, LocalDateTime pickedUpAt,
                     LocalDateTime inTransitAt, LocalDateTime deliveredAt, LocalDateTime cancelledAt,
                     String cancellationReason) {
        this.id = id;
        this.orderId = orderId;
        this.pickupAddress = pickupAddress;
        this.deliveryAddress = deliveryAddress;
        this.deliveryPersonId = deliveryPersonId;
        this.status = status;
        this.availableForSelfAssignment = availableForSelfAssignment;
        this.assignedAt = assignedAt;
        this.readyAt = readyAt;
        this.pickedUpAt = pickedUpAt;
        this.inTransitAt = inTransitAt;
        this.deliveredAt = deliveredAt;
        this.cancelledAt = cancelledAt;
        this.cancellationReason = cancellationReason;
        validateConsistency();
    }

    public static Delivery rehydrate(DeliveryId id, OrderId orderId, Address pickup, Address delivery,
                                     DeliveryPersonId person, DeliveryStatus status, boolean available,
                                     LocalDateTime assignedAt, LocalDateTime readyAt, LocalDateTime pickedUpAt,
                                     LocalDateTime inTransitAt, LocalDateTime deliveredAt, LocalDateTime cancelledAt,
                                     String cancellationReason) {
        return new Delivery(id, orderId, pickup, delivery, person, status, available,
                assignedAt, readyAt, pickedUpAt, inTransitAt, deliveredAt, cancelledAt, cancellationReason);
    }

    // ---- Behaviour ---------------------------------------------------------

    /** US27/US32 — single courier per delivery. */
    public void selfAssign(DeliveryPersonId personId, LocalDateTime now) {
        requireNonNull(personId, "personId");
        requireNonNull(now, "now");
        if (status != DeliveryStatus.PENDING && status != DeliveryStatus.READY_FOR_PICKUP) {
            throw new IllegalStateException("Cannot self-assign — delivery is " + status);
        }
        if (deliveryPersonId != null) {
            throw new be.kdg.backend.domain.delivery.DeliveryAlreadyAssignedException(id);
        }
        if (!availableForSelfAssignment) {
            throw new IllegalStateException("Delivery " + id + " is not available for assignment");
        }
        this.deliveryPersonId = personId;
        this.availableForSelfAssignment = false;
        this.assignedAt = now;
        // Claiming an already-ready delivery keeps it READY_FOR_PICKUP so the courier
        // can pick it up right away (US28/US30).
        this.status = status == DeliveryStatus.READY_FOR_PICKUP
                ? DeliveryStatus.READY_FOR_PICKUP
                : DeliveryStatus.ASSIGNED;
    }

    /** Called on consuming {@code order.ready_for_pickup} event. */
    public void onOrderReadyForPickup(LocalDateTime now) {
        if (status != DeliveryStatus.ASSIGNED && status != DeliveryStatus.PENDING) {
            throw new IllegalStateException("Cannot mark ready — delivery state=" + status);
        }
        requireNonNull(now, "now");
        this.readyAt = now;
        if (status == DeliveryStatus.PENDING) {
            // Courier has not claimed yet — delivery remains claimable now until courier claims.
            // US29: once claimed AND order is ready, courier must complete (US30).
            this.status = DeliveryStatus.READY_FOR_PICKUP;
            // keep availableForSelfAssignment = true so courier CAN still claim after ready
        } else if (status == DeliveryStatus.ASSIGNED) {
            this.status = DeliveryStatus.READY_FOR_PICKUP;
        }
    }

    public void markPickedUp(LocalDateTime now) {
        requireStatus(DeliveryStatus.READY_FOR_PICKUP, "markPickedUp");
        requireNonNull(now, "now");
        this.pickedUpAt = now;
        this.status = DeliveryStatus.PICKED_UP;
    }

    public void markInTransit(LocalDateTime now) {
        requireStatus(DeliveryStatus.PICKED_UP, "markInTransit");
        requireNonNull(now, "now");
        this.inTransitAt = now;
        this.status = DeliveryStatus.IN_TRANSIT;
    }

    public void markDelivered(LocalDateTime now) {
        if (status != DeliveryStatus.IN_TRANSIT && status != DeliveryStatus.PICKED_UP) {
            throw new IllegalStateException("Cannot deliver — delivery state=" + status);
        }
        requireNonNull(now, "now");
        this.deliveredAt = now;
        this.status = DeliveryStatus.DELIVERED;
    }

    /** US29 — cancel only while status == ASSIGNED. Once READY_FOR_PICKUP, courier MUST finish (US30). */
    public void cancelClaim(String reason, LocalDateTime now) {
        requireNonBlank(reason, "reason");
        requireNonNull(now, "now");
        if (status != DeliveryStatus.ASSIGNED) {
            throw new IllegalStateException("Cannot cancel claim — delivery is " + status
                    + " (US29: cancellation allowed only while ASSIGNED)");
        }
        this.status = DeliveryStatus.CANCELLED;
        this.cancellationReason = reason;
        this.cancelledAt = now;
        // Delivery person release happens via Spring ApplicationEvent in service layer.
    }

    // ---- Queries -----------------------------------------------------------

    public boolean isAvailableForSelfAssignment() {
        return availableForSelfAssignment && deliveryPersonId == null;
    }
    public boolean hasCourier() { return deliveryPersonId != null; }
    public boolean isTerminal() {
        return status == DeliveryStatus.DELIVERED || status == DeliveryStatus.CANCELLED;
    }

    public DeliveryId id()                          { return id; }
    public OrderId orderId()                        { return orderId; }
    public Address pickupAddress()                  { return pickupAddress; }
    public Address deliveryAddress()                { return deliveryAddress; }
    public DeliveryPersonId deliveryPersonId()      { return deliveryPersonId; }
    public DeliveryStatus status()                  { return status; }
    public LocalDateTime assignedAt()               { return assignedAt; }
    public LocalDateTime readyAt()                  { return readyAt; }
    public LocalDateTime pickedUpAt()               { return pickedUpAt; }
    public LocalDateTime inTransitAt()              { return inTransitAt; }
    public LocalDateTime deliveredAt()              { return deliveredAt; }
    public LocalDateTime cancelledAt()              { return cancelledAt; }
    public String cancellationReason()              { return cancellationReason; }

    // ---- Helpers -----------------------------------------------------------

    private void requireStatus(DeliveryStatus expected, String op) {
        if (status != expected) {
            throw new IllegalStateException("Cannot " + op + " — expected " + expected + " but was " + status);
        }
    }
    private void validateConsistency() {
        if (status == DeliveryStatus.ASSIGNED && deliveryPersonId == null) {
            throw new IllegalStateException("ASSIGNED delivery must have a delivery person");
        }
        if (status == DeliveryStatus.DELIVERED && deliveredAt == null) {
            throw new IllegalStateException("DELIVERED delivery must have deliveredAt");
        }
        if (status == DeliveryStatus.CANCELLED && cancellationReason == null) {
            throw new IllegalStateException("CANCELLED delivery must have a reason");
        }
    }
    private static void requireNonNull(Object v, String name) {
        if (v == null) throw new IllegalArgumentException(name + " must not be null");
    }
    private static void requireNonBlank(String v, String name) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Delivery other)) return false;
        return id.equals(other.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
}