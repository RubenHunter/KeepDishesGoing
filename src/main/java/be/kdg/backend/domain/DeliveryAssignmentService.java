package be.kdg.backend.domain;

import org.jmolecules.ddd.annotation.Service;

@Service
public class DeliveryAssignmentService {

    public boolean canAssignDelivery(DeliveryPerson deliveryPerson, Delivery delivery, double maxRadiusKm) {
        if (!deliveryPerson.canAcceptDelivery()) {
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

    private Location estimateRestaurantLocation(Address address) {
        return new Location(52.3676, 4.9041); // Simplified - would use geocoding service
    }

    private Location estimateDeliveryLocation(Address address) {
        return new Location(52.3791, 4.9003); // Simplified - would use geocoding service
    }
}