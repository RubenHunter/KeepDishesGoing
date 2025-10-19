package be.kdg.backend.domain;

import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
public class DeliveryPerson {
    @Identity
    private final DeliveryPersonId id;
    private final PersonName name;
    private final VehicleType vehicleType;
    private boolean isAvailable;
    private Location currentLocation;
    private DeliveryId assignedDeliveryId;
    private LocalDateTime assignmentTime;

    public DeliveryPerson(DeliveryPersonId id, PersonName name, VehicleType vehicleType,
                          boolean isAvailable, Location currentLocation) {
        validateConstructor(id, name, vehicleType, currentLocation);

        this.id = id;
        this.name = name;
        this.vehicleType = vehicleType;
        this.isAvailable = isAvailable;
        this.currentLocation = currentLocation;
        this.assignedDeliveryId = null;
        this.assignmentTime = null;
    }

    private void validateConstructor(DeliveryPersonId id, PersonName name,
                                     VehicleType vehicleType, Location currentLocation) {
        if (id == null) throw new IllegalArgumentException("DeliveryPerson ID cannot be null");
        if (name == null) throw new IllegalArgumentException("Person name cannot be null");
        if (vehicleType == null) throw new IllegalArgumentException("Vehicle type cannot be null");
        if (currentLocation == null) throw new IllegalArgumentException("Current location cannot be null");
    }

    public void updateAvailability(boolean available) {
        if (!available && hasActiveAssignment()) {
            throw new DeliveryPersonAlreadyAssignedException(this.id);
        }
        this.isAvailable = available;
    }

    public void assignDelivery(DeliveryId deliveryId) {
        if (hasActiveAssignment()) {
            throw new DeliveryPersonAlreadyAssignedException(this.id);
        }
        if (!isAvailable) {
            throw new IllegalStateException("Delivery person is not available for assignment");
        }

        this.assignedDeliveryId = deliveryId;
        this.assignmentTime = LocalDateTime.now();
        this.isAvailable = false;
    }

    public void unassignDelivery() {
        this.assignedDeliveryId = null;
        this.assignmentTime = null;
        this.isAvailable = true;
    }

    public void updateLocation(Location newLocation) {
        if (newLocation == null) {
            throw new IllegalArgumentException("New location cannot be null");
        }
        this.currentLocation = newLocation;
    }

    public boolean canAcceptDelivery() {
        return isAvailable && !hasActiveAssignment();
    }

    public boolean hasActiveAssignment() {
        return assignedDeliveryId != null;
    }

    public boolean isWithinDeliveryRadius(Location restaurantLocation, double maxRadiusKm) {
        double distance = currentLocation.calculateDistance(restaurantLocation);
        return distance <= maxRadiusKm;
    }

    public boolean isVehicleSuitableForDistance(double distanceKm) {
        switch (vehicleType) {
            case BICYCLE: return distanceKm <= 5.0;
            case SCOOTER: return distanceKm <= 15.0;
            case WALKING: return distanceKm <= 2.0;
            case CAR: return distanceKm <= 50.0;
            default: return false;
        }
    }

    public DeliveryPersonId getId() { return id; }
    public PersonName getName() { return name; }
    public VehicleType getVehicleType() { return vehicleType; }
    public boolean isAvailable() { return isAvailable; }
    public Location getCurrentLocation() { return currentLocation; }
    public DeliveryId getAssignedDeliveryId() { return assignedDeliveryId; }
    public LocalDateTime getAssignmentTime() { return assignmentTime; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeliveryPerson that = (DeliveryPerson) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}