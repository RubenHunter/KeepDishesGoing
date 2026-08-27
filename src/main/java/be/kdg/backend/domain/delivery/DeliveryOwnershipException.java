package be.kdg.backend.domain.delivery;

/**
 * US29/US30 — a courier may only act on a delivery that is currently assigned to them.
 * Thrown when the requester's identity does not match the delivery's assigned courier.
 */
public class DeliveryOwnershipException extends RuntimeException {

    public DeliveryOwnershipException(String message) {
        super(message);
    }
}
