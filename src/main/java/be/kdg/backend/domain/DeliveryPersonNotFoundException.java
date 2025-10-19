package be.kdg.backend.domain;

public class DeliveryPersonNotFoundException extends RuntimeException {
    public DeliveryPersonNotFoundException(String message) {
        super(message);
    }

    public DeliveryPersonNotFoundException(DeliveryPersonId id) {
        super("Delivery person not found: " + id.value());
    }
}
