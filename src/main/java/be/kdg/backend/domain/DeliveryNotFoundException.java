package be.kdg.backend.domain;

public class DeliveryNotFoundException extends RuntimeException {
    public DeliveryNotFoundException(String message) {
        super(message);
    }

    public DeliveryNotFoundException(DeliveryId id) {
        super("Delivery not found: " + id.value());
    }
}