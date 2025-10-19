package be.kdg.backend.domain;

import org.jmolecules.ddd.annotation.Service;

@Service
public class DeliveryAssignmentService {

    public boolean canAssignDelivery(DeliveryPerson deliveryPerson, Delivery delivery, double maxRadiusKm) {
        if (!deliveryPerson.canAcceptDelivery()) {
            throw new DeliveryPersonAlreadyAssignedException(deliveryPerson.getId());
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

    private Location estimateRestaurantLocation(Address address) {
        return new Location(52.3676, 4.9041);
    }

    private Location estimateDeliveryLocation(Address address) {
        return new Location(52.3791, 4.9003);
    }
}