package be.kdg.backend.infrastructure.payment;

import be.kdg.backend.application.payment.PaymentGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory stub payment gateway (US20) for dev. Stores paymentRef → status.
 * Real provider (Mollie/Stripe) would implement the same port without changing order/application code.
 *
 * <p>Dev flow (T3): {@code startPayment} returns {@code redirectUrl = null} — there is no hosted
 * payment page, so the frontend falls back to its in-app "Pay" button. Confirmation is done via the
 * shared-secret webhook ({@code PATCH /api/payments/{ref}/status}), mirroring the Stripe flow.
 */
@Component
@ConditionalOnProperty(name = "kdg.payment.provider", havingValue = "stub", matchIfMissing = true)
public class StubPaymentGateway implements PaymentGateway {

    private final Map<String, PaymentConfirmation.PaymentStatus> store = new ConcurrentHashMap<>();

    @Override
    public StartPaymentResponse startPayment(StartPaymentRequest request) {
        String ref = "pay_" + UUID.randomUUID();
        // For dev: stub returns an awaiting payment that becomes PAID when /confirm is called.
        store.put(ref, PaymentConfirmation.PaymentStatus.PAID);
        return new StartPaymentResponse(ref, null);
    }

    @Override
    public PaymentConfirmation confirm(String paymentRef) {
        PaymentConfirmation.PaymentStatus status = store.getOrDefault(
                paymentRef, PaymentConfirmation.PaymentStatus.FAILED);
        return new PaymentConfirmation(paymentRef, status);
    }
}
