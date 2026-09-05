package be.kdg.backend.domain;

/**
 * Thrown when the payment webhook call lacks the shared-secret header (T3). Mapped to 403 by
 * the global advice — a caller without the configured signature must not mutate order payment state.
 */
public class PaymentSignatureException extends DomainException {
    public PaymentSignatureException(String message) { super(message); }
}
