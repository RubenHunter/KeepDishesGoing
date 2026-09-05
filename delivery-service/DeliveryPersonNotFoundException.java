package be.kdg.backend.domain.driver;

import be.kdg.backend.domain.shared.DeliveryPersonId;

public class DeliveryPersonNotFoundException extends RuntimeException {
    public DeliveryPersonNotFoundException(DeliveryPersonId id) {
        super("Delivery person not found: " + id.value());
    }
}