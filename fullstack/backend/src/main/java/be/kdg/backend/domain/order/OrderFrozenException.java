package be.kdg.backend.domain.order;

import be.kdg.backend.domain.DomainException;

/** Thrown when an attempt is made to mutate an order that has already been placed (US18). */
public class OrderFrozenException extends DomainException {
    public OrderFrozenException(String message) { super(message); }
}