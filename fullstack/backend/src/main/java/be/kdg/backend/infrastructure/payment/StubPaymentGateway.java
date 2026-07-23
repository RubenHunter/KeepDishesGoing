package be.kdg.backend.infrastructure.payment;

import be.kdg.backend.application.payment.PaymentGateway;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory stub payment gateway (US20) for dev. Stores paymentRef → status.
 * Real provider (Mollie/Stripe) would implement the same port without changing order/application code.
 */
@Component
public class StubPaymentGateway implements PaymentGateway {

    private final Map<String, PaymentConfirmation.PaymentStatus> store = new ConcurrentHashMap<>();

    @Override
    public StartPaymentResponse startPayment(StartPaymentRequest request) {
        String ref = "pay_" + UUID.randomUUID();
        // For dev: stub returns an awaiting payment that becomes PAID when /confirm is called.
        store.put(ref, PaymentConfirmation.PaymentStatus.PAID);
        String redirect = "http://localhost:8081/api/payments/" + ref + "/confirm";
        return new StartPaymentResponse(ref, redirect);
    }

    @Override
    public PaymentConfirmation confirm(String paymentRef) {
        PaymentConfirmation.PaymentStatus status = store.getOrDefault(
                paymentRef, PaymentConfirmation.PaymentStatus.FAILED);
        return new PaymentConfirmation(paymentRef, status);
    }
}