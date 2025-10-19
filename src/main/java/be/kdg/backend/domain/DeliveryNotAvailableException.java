package be.kdg.backend.domain;

import be.kdg.backend.domain.DeliveryId;

public class DeliveryNotAvailableException extends RuntimeException {
    public DeliveryNotAvailableException(String message) {
        super(message);
    }

    public DeliveryNotAvailableException(DeliveryId deliveryId) {
        super("Delivery is not available for assignment: " + deliveryId.value());
    }
}