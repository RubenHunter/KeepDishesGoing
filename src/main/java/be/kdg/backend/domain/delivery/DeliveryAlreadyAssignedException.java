package be.kdg.backend.domain.delivery;

import be.kdg.backend.domain.shared.DeliveryId;

/** Thrown when a courier tries to self-assign an already-assigned delivery (US32). */
public class DeliveryAlreadyAssignedException extends RuntimeException {
    public DeliveryAlreadyAssignedException(DeliveryId id) {
        super("Delivery " + id.value() + " is already assigned to another courier");
    }
}