package be.kdg.backend.infrastructure.payment;

import be.kdg.backend.application.PaymentProperties;
import be.kdg.backend.application.payment.PaymentGateway;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory stub payment gateway (US20) for dev. Stores paymentRef → status.
 * Real provider (Mollie/Stripe) would implement the same port without changing order/application code.
 *
 * <p>Dev flow (T3): {@code startPayment} returns a redirect to the frontend tracking page. Payment
 * confirmation is NOT done by the browser landing on a URL; it is done via {@code order-service.http}
 * (or any manual POST) sending the shared-secret header {@code kdg.payment.webhook-secret-header}.
 */
@Component
public class StubPaymentGateway implements PaymentGateway {

    private static final String ORDER_ID_PLACEHOLDER = "{orderId}";

    private final Map<String, PaymentConfirmation.PaymentStatus> store = new ConcurrentHashMap<>();
    private final PaymentProperties paymentProperties;

    public StubPaymentGateway(PaymentProperties paymentProperties) {
        this.paymentProperties = paymentProperties;
    }

    @Override
    public StartPaymentResponse startPayment(StartPaymentRequest request) {
        String ref = "pay_" + UUID.randomUUID();
        // For dev: stub returns an awaiting payment that becomes PAID when /confirm is called.
        store.put(ref, PaymentConfirmation.PaymentStatus.PAID);
        String redirect = paymentProperties.stub().redirectTemplate()
                .replace(ORDER_ID_PLACEHOLDER, request.orderId());
        return new StartPaymentResponse(ref, redirect);
    }

    @Override
    public PaymentConfirmation confirm(String paymentRef) {
        PaymentConfirmation.PaymentStatus status = store.getOrDefault(
                paymentRef, PaymentConfirmation.PaymentStatus.FAILED);
        return new PaymentConfirmation(paymentRef, status);
    }
}
