package be.kdg.backend.domain;

import org.jmolecules.ddd.annotation.Service;

@Service
public class DeliveryAssignmentService {

    public boolean canAssignDelivery(DeliveryPerson deliveryPerson, Delivery delivery, double maxRadiusKm) {
        if (deliveryPerson.hasActiveAssignment()) {
            return false;
        }

        if (delivery.hasAssignedDeliveryPerson()) {
            return false;
        }

        if (!delivery.isAvailableForSelfAssignment()) {
            return false;
        }

        Location restaurantLocation = estimateRestaurantLocation(delivery.getPickupAddress());
        if (!deliveryPerson.isWithinDeliveryRadius(restaurantLocation, maxRadiusKm)) {
            return false;
        }

        Location deliveryLocation = estimateDeliveryLocation(delivery.getDeliveryAddress());
        double totalDistance = restaurantLocation.calculateDistance(deliveryLocation);

        return deliveryPerson.isVehicleSuitableForDistance(totalDistance);
    }

    public boolean canSelfAssignDelivery(DeliveryPerson deliveryPerson, Delivery delivery, double maxRadiusKm) {
        if (deliveryPerson.hasActiveAssignment()) {
            throw new DeliveryPersonAlreadyAssignedException(deliveryPerson.getId());
        }

        if (delivery.hasAssignedDeliveryPerson()) {
            throw new DeliveryAlreadyAssignedException(delivery.getId());
        }

        if (!delivery.isAvailableForSelfAssignment()) {
            throw new DeliveryNotAvailableException(delivery.getId());
        }

        Location restaurantLocation = estimateRestaurantLocation(delivery.getPickupAddress());
        if (!deliveryPerson.isWithinDeliveryRadius(restaurantLocation, maxRadiusKm)) {
            return false;
        }

        Location deliveryLocation = estimateDeliveryLocation(delivery.getDeliveryAddress());
        double totalDistance = restaurantLocation.calculateDistance(deliveryLocation);

        return deliveryPerson.isVehicleSuitableForDistance(totalDistance);
    }

    public void validateSingleActiveAssignment(DeliveryPerson deliveryPerson) {
        if (deliveryPerson.hasActiveAssignment()) {
            throw new DeliveryPersonAlreadyAssignedException(deliveryPerson.getId());
        }
    }

    public void validateSingleDeliveryPersonPerDelivery(Delivery delivery) {
        if (delivery.hasAssignedDeliveryPerson()) {
            throw new DeliveryAlreadyAssignedException(delivery.getId());
        }
    }

    public void validateDeliveryAvailableForAssignment(Delivery delivery) {
        if (!delivery.isAvailableForSelfAssignment()) {
            throw new DeliveryNotAvailableException(delivery.getId());
        }
    }

    private Location estimateRestaurantLocation(Address address) {
        return new Location(52.3676, 4.9041);
    }

    private Location estimateDeliveryLocation(Address address) {
        return new Location(52.3791, 4.9003);
    }
}