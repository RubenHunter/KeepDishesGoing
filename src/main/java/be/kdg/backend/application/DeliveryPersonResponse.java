package be.kdg.backend.application;

import be.kdg.backend.domain.DeliveryPerson;

public record DeliveryPersonResponse(
        String deliveryPersonId,
        String name,
        String vehicleType,
        boolean isAvailable,
        double latitude,
        double longitude,
        String assignedDeliveryId
) {
    public static DeliveryPersonResponse fromDomain(DeliveryPerson deliveryPerson) {
        return new DeliveryPersonResponse(
                deliveryPerson.getId().value(),
                deliveryPerson.getName().value(),
                deliveryPerson.getVehicleType().name(),
                deliveryPerson.isAvailable(),
                deliveryPerson.getCurrentLocation().latitude(),
                deliveryPerson.getCurrentLocation().longitude(),
                deliveryPerson.getAssignedDeliveryId() != null ? deliveryPerson.getAssignedDeliveryId().value() : null
        );
    }
}