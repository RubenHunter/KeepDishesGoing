package be.kdg.backend.domain.order;

import be.kdg.backend.domain.DomainException;

/** US11 — thrown when an order is placed while the restaurant is closed. */
public class RestaurantClosedException extends DomainException {
    public RestaurantClosedException(String message) { super(message); }
}
