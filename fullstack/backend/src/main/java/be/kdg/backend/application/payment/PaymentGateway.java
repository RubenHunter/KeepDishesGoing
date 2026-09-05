package be.kdg.backend.application.payment;

/**
 * Payment provider port (US20). Implemented by stub in dev.
 */
public interface PaymentGateway {

    /** Start payment for an order; returns a redirect URL + reference id (provider-specific). */
    StartPaymentResponse startPayment(StartPaymentRequest request);

    /** Confirm payment (called by provider webhook or dev stub endpoint). */
    PaymentConfirmation confirm(String paymentRef);

    record StartPaymentRequest(String orderId, double amount, String currency) {}

    record StartPaymentResponse(String paymentRef, String redirectUrl) {}

    record PaymentConfirmation(String paymentRef, PaymentStatus status) {
        public enum PaymentStatus { PAID, FAILED }
    }
}