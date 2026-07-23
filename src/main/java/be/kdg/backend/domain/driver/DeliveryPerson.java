package be.kdg.backend.domain.driver;

import be.kdg.backend.domain.shared.DeliveryId;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * DeliveryPerson (driver) aggregate root. Single-active-assignment rule (US31).
 *
 * Lifecycle:
 *   created with no assignment.
 *   assignDelivery(deliveryId, now)     — sets assignedDeliveryId, throws if already has one.   (US27 fired by Delivery.selfAssign listener)
 *   release(now)                         — clears assignment (US29 on cancel, US30 on delivered).
 *   updateAvailability(true|false)
 *
 * Refers to a {@link Delivery} by ID — they NEVER share a transaction (coding-mistakes #3).
 */
@AggregateRoot
public class DeliveryPerson {

    @Identity
    private final DeliveryPersonId id;
    private final String name;
    private final String vehicleType;

    private boolean available;
    private DeliveryId assignedDeliveryId;
    private LocalDateTime assignmentTime;
    private LocalDateTime updatedAt;

    public DeliveryPerson(DeliveryPersonId id, String name, String vehicleType, boolean available) {
        requireNonNull(id, "id");
        requireNonBlank(name, "name");
        requireNonBlank(vehicleType, "vehicleType");
        this.id = id;
        this.name = name;
        this.vehicleType = vehicleType;
        this.available = available;
        this.updatedAt = LocalDateTime.now();
    }

    private DeliveryPerson(DeliveryPersonId id, String name, String vehicleType, boolean available,
                          DeliveryId assignedDeliveryId, LocalDateTime assignmentTime, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.vehicleType = vehicleType;
        this.available = available;
        this.assignedDeliveryId = assignedDeliveryId;
        this.assignmentTime = assignmentTime;
        this.updatedAt = updatedAt;
        validateConsistency();
    }

    public static DeliveryPerson rehydrate(DeliveryPersonId id, String name, String vehicleType, boolean available,
                                            DeliveryId assignedDeliveryId, LocalDateTime assignmentTime,
                                            LocalDateTime updatedAt) {
        return new DeliveryPerson(id, name, vehicleType, available, assignedDeliveryId, assignmentTime, updatedAt);
    }

    /** US27 halves the assignment: DeliveryPerson records the delivery id (US31 ensures single). */
    public void assignDelivery(DeliveryId deliveryId, LocalDateTime now) {
        requireNonNull(deliveryId, "deliveryId");
        requireNonNull(now, "now");
        if (assignedDeliveryId != null) {
            throw new DeliveryPersonAlreadyAssignedException(this.id);
        }
        this.assignedDeliveryId = deliveryId;
        this.assignmentTime = now;
        this.available = false;
        touch();
    }

    /** Music on cancel (US29) or on delivered (US30). Driver returns to pool. */
    public void release(LocalDateTime now) {
        requireNonNull(now, "now");
        this.assignedDeliveryId = null;
        this.assignmentTime = null;
        this.available = true;
        touch();
    }

    public void updateAvailability(boolean available, LocalDateTime now) {
        if (!available && assignedDeliveryId != null) {
            throw new IllegalStateException("Cannot mark unavailable — driver " + id + " has active delivery " + assignedDeliveryId);
        }
        this.available = available;
        touch();
    }

    public boolean hasActiveAssignment() { return assignedDeliveryId != null; }
    public boolean canAccept()           { return available && assignedDeliveryId == null; }

    public DeliveryPersonId id()              { return id; }
    public String name()                       { return name; }
    public String vehicleType()                { return vehicleType; }
    public boolean available()                 { return available; }
    public DeliveryId assignedDeliveryId()    { return assignedDeliveryId; }
    public LocalDateTime assignmentTime()      { return assignmentTime; }
    public LocalDateTime updatedAt()          { return updatedAt; }

    private void validateConsistency() {
        if (assignedDeliveryId != null && !available) { /* expected */ return; }
        if (assignedDeliveryId == null && assignmentTime != null) {
            throw new IllegalStateException("assignmentTime set without an assignment");
        }
    }
    private void touch() { this.updatedAt = LocalDateTime.now(); }
    private static void requireNonNull(Object v, String n) { if (v == null) throw new IllegalArgumentException(n + " must not be null"); }
    private static void requireNonBlank(String v, String n) { if (v == null || v.isBlank()) throw new IllegalArgumentException(n + " must not be blank"); }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeliveryPerson other)) return false;
        return id.equals(other.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
}