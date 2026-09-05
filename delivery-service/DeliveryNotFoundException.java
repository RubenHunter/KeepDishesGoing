package be.kdg.backend.domain.delivery;

import be.kdg.backend.domain.shared.DeliveryId;

/** Delivery not found — domain exception. */
public class DeliveryNotFoundException extends RuntimeException {
    public DeliveryNotFoundException(DeliveryId id) {
        super("Delivery not found: " + id.value());
    }
}