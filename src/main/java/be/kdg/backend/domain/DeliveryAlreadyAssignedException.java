package be.kdg.backend.domain;

public class DeliveryAlreadyAssignedException extends RuntimeException {
    public DeliveryAlreadyAssignedException(String message) {
        super(message);
    }

    public DeliveryAlreadyAssignedException(DeliveryId deliveryId) {
        super("Delivery already has an assigned delivery person: " + deliveryId.value());
    }
}