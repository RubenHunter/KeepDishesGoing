package be.kdg.backend.domain.order;

import be.kdg.backend.domain.DomainException;

/**
 * Thrown when a customer attempts to place or cancel an order they do not own
 * (requester derived from the JWT subject does not match {@code Order.customerId()}).
 */
public class OrderOwnershipException extends DomainException {
    public OrderOwnershipException(String message) {
        super(message);
    }
}
