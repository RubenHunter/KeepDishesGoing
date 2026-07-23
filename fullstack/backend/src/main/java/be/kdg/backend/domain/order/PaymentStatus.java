package be.kdg.backend.domain.order;

/** Payment lifecycle on an order. */
public enum PaymentStatus {
    AWAITING,
    PAID,
    FAILED
}