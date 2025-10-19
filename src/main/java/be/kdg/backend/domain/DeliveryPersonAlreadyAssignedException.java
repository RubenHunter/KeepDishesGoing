package be.kdg.backend.domain;

public class DeliveryPersonAlreadyAssignedException extends RuntimeException {
    public DeliveryPersonAlreadyAssignedException(String message) {
        super(message);
    }

    public DeliveryPersonAlreadyAssignedException(DeliveryPersonId deliveryPersonId) {
        super("Delivery person already has an active assignment: " + deliveryPersonId.value());
    }
}