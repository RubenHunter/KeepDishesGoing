package be.kdg.backend.domain;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.time.LocalDateTime;
import java.util.Objects;

@AggregateRoot
public class Delivery {
    @Identity
    private final DeliveryId id;
    private final OrderId orderId;
    private DeliveryPersonId deliveryPersonId;
    private final Address pickupAddress;
    private final Address deliveryAddress;
    private DeliveryStatus status;
    private LocalDateTime estimatedDeliveryTime;
    private final LocalDateTime assignedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
    private CancellationReason cancellationReason;

    public Delivery(DeliveryId id, OrderId orderId, Address pickupAddress, Address deliveryAddress) {
        validateConstructor(id, orderId, pickupAddress, deliveryAddress);

        this.id = id;
        this.orderId = orderId;
        this.pickupAddress = pickupAddress;
        this.deliveryAddress = deliveryAddress;
        this.status = DeliveryStatus.PENDING;
        this.estimatedDeliveryTime = null;
        this.assignedAt = null;
        this.pickedUpAt = null;
        this.deliveredAt = null;
        this.cancellationReason = null;
        this.deliveryPersonId = null;
    }

    private Delivery(DeliveryId id, OrderId orderId, DeliveryPersonId deliveryPersonId,
                     Address pickupAddress, Address deliveryAddress, DeliveryStatus status,
                     LocalDateTime estimatedDeliveryTime, LocalDateTime assignedAt,
                     LocalDateTime pickedUpAt, LocalDateTime deliveredAt,
                     CancellationReason cancellationReason) {
        this.id = id;
        this.orderId = orderId;
        this.deliveryPersonId = deliveryPersonId;
        this.pickupAddress = pickupAddress;
        this.deliveryAddress = deliveryAddress;
        this.status = status;
        this.estimatedDeliveryTime = estimatedDeliveryTime;
        this.assignedAt = assignedAt;
        this.pickedUpAt = pickedUpAt;
        this.deliveredAt = deliveredAt;
        this.cancellationReason = cancellationReason;
    }

    public static Delivery reconstruct(DeliveryId id, OrderId orderId, DeliveryPersonId deliveryPersonId,
                                       Address pickupAddress, Address deliveryAddress, DeliveryStatus status,
                                       LocalDateTime estimatedDeliveryTime, LocalDateTime assignedAt,
                                       LocalDateTime pickedUpAt, LocalDateTime deliveredAt,
                                       CancellationReason cancellationReason) {
        Delivery delivery = new Delivery(id, orderId, deliveryPersonId, pickupAddress, deliveryAddress,
                status, estimatedDeliveryTime, assignedAt, pickedUpAt,
                deliveredAt, cancellationReason);
        delivery.validateDeliveryConsistency();
        return delivery;
    }

    private void validateConstructor(DeliveryId id, OrderId orderId, Address pickupAddress, Address deliveryAddress) {
        if (id == null) throw new IllegalArgumentException("Delivery ID cannot be null");
        if (orderId == null) throw new IllegalArgumentException("Order ID cannot be null");
        if (pickupAddress == null) throw new IllegalArgumentException("Pickup address cannot be null");
        if (deliveryAddress == null) throw new IllegalArgumentException("Delivery address cannot be null");
    }

    public void assignDeliveryPerson(DeliveryPersonId personId, LocalDateTime assignedAt) {
        validateDeliveryAssignment();
        validateNoExistingAssignment();

        if (personId == null) {
            throw new IllegalArgumentException("Delivery person ID cannot be null");
        }
        if (assignedAt == null) {
            throw new IllegalArgumentException("Assignment time cannot be null");
        }
        if (assignedAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Assignment time cannot be in the past");
        }

        this.deliveryPersonId = personId;
        this.status = DeliveryStatus.ASSIGNED;
        this.estimatedDeliveryTime = calculateEstimatedTime();
    }

    public void markPickedUp() {
        validateStatusTransition(DeliveryStatus.PICKED_UP);

        this.status = DeliveryStatus.PICKED_UP;
        this.pickedUpAt = LocalDateTime.now();
        this.estimatedDeliveryTime = calculateEstimatedTime();
    }

    public void markInTransit() {
        validateStatusTransition(DeliveryStatus.IN_TRANSIT);

        if (status != DeliveryStatus.PICKED_UP) {
            throw new IllegalStateException("Delivery must be picked up before marking in transit");
        }

        this.status = DeliveryStatus.IN_TRANSIT;
        this.estimatedDeliveryTime = calculateEstimatedTime();
    }

    public void markDelivered() {
        validateStatusTransition(DeliveryStatus.DELIVERED);

        if (status != DeliveryStatus.IN_TRANSIT && status != DeliveryStatus.PICKED_UP) {
            throw new IllegalStateException("Delivery must be in transit or picked up before marking delivered");
        }

        this.status = DeliveryStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
        this.deliveryPersonId = null;
    }

    public void cancelDelivery(CancellationReason reason) {
        validateStatusTransition(DeliveryStatus.CANCELLED);

        if (reason == null) {
            throw new IllegalArgumentException("Cancellation reason cannot be null");
        }

        this.status = DeliveryStatus.CANCELLED;
        this.cancellationReason = reason;
        this.deliveryPersonId = null;
    }

    public boolean hasAssignedDeliveryPerson() {
        return deliveryPersonId != null;
    }

    public DeliveryPersonId getAssignedDeliveryPersonId() {
        return deliveryPersonId;
    }

    public LocalDateTime calculateEstimatedTime() {
        LocalDateTime baseTime = LocalDateTime.now();

        if (status == DeliveryStatus.PICKED_UP || status == DeliveryStatus.IN_TRANSIT) {
            Location pickupLocation = new Location(52.3676, 4.9041);
            Location deliveryLocation = new Location(52.3791, 4.9003);
            double distance = pickupLocation.calculateDistance(deliveryLocation);

            double speedKmH = getAverageSpeed();
            double estimatedHours = distance / speedKmH;

            return baseTime.plusMinutes((long) (estimatedHours * 60));
        }

        return baseTime.plusMinutes(30);
    }

    private double getAverageSpeed() {
        return 15.0;
    }

    private void validateDeliveryAssignment() {
        if (status != DeliveryStatus.PENDING) {
            throw new IllegalStateException("Only pending deliveries can be assigned");
        }
    }

    private void validateNoExistingAssignment() {
        if (hasAssignedDeliveryPerson()) {
            throw new DeliveryAlreadyAssignedException(this.id);
        }
    }

    private void validateStatusTransition(DeliveryStatus newStatus) {
        if (status == DeliveryStatus.DELIVERED) {
            throw new IllegalStateException("Cannot modify a delivered delivery");
        }
        if (status == DeliveryStatus.CANCELLED) {
            throw new IllegalStateException("Cannot modify a cancelled delivery");
        }

        switch (newStatus) {
            case ASSIGNED:
                if (status != DeliveryStatus.PENDING) {
                    throw new IllegalStateException("Can only assign pending deliveries");
                }
                break;
            case PICKED_UP:
                if (status != DeliveryStatus.ASSIGNED) {
                    throw new IllegalStateException("Can only mark assigned deliveries as picked up");
                }
                break;
            case IN_TRANSIT:
                if (status != DeliveryStatus.PICKED_UP) {
                    throw new IllegalStateException("Can only mark picked up deliveries as in transit");
                }
                break;
            case DELIVERED:
                if (status != DeliveryStatus.IN_TRANSIT && status != DeliveryStatus.PICKED_UP) {
                    throw new IllegalStateException("Can only mark in transit or picked up deliveries as delivered");
                }
                break;
            case CANCELLED:
                if (status == DeliveryStatus.DELIVERED) {
                    throw new IllegalStateException("Cannot cancel a delivered delivery");
                }
                break;
        }
    }

    private void validateDeliveryConsistency() {
        if (status == DeliveryStatus.ASSIGNED && deliveryPersonId == null) {
            throw new IllegalStateException("Assigned delivery must have a delivery person");
        }
        if (status == DeliveryStatus.DELIVERED && deliveredAt == null) {
            throw new IllegalStateException("Delivered delivery must have delivery timestamp");
        }
        if (status == DeliveryStatus.CANCELLED && cancellationReason == null) {
            throw new IllegalStateException("Cancelled delivery must have cancellation reason");
        }
        if (estimatedDeliveryTime != null && estimatedDeliveryTime.isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Estimated delivery time must be in the future");
        }
    }

    public DeliveryId getId() { return id; }
    public OrderId getOrderId() { return orderId; }
    public DeliveryPersonId getDeliveryPersonId() { return deliveryPersonId; }
    public Address getPickupAddress() { return pickupAddress; }
    public Address getDeliveryAddress() { return deliveryAddress; }
    public DeliveryStatus getStatus() { return status; }
    public LocalDateTime getEstimatedDeliveryTime() { return estimatedDeliveryTime; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public LocalDateTime getPickedUpAt() { return pickedUpAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public CancellationReason getCancellationReason() { return cancellationReason; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Delivery delivery = (Delivery) o;
        return Objects.equals(id, delivery.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}