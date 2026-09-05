package be.kdg.backend.domain;

/**
 * Thrown when a Stripe webhook call fails signature verification. Mapped to 400 by the global advice —
 * a caller whose {@code Stripe-Signature} header does not match the configured webhook secret must
 * not mutate order payment state.
 */
public class StripeSignatureException extends DomainException {
    public StripeSignatureException(String message) { super(message); }
}
