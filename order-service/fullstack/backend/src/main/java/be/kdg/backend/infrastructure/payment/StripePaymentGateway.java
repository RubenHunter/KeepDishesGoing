package be.kdg.backend.infrastructure.payment;

import be.kdg.backend.application.PaymentProperties;
import be.kdg.backend.application.payment.PaymentGateway;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Stripe Checkout Session (hosted) gateway — test mode only (US20). Implements the same
 * {@link PaymentGateway} port as the stub; active when {@code kdg.payment.provider=stripe}.
 *
 * <p>The card/checkout page is hosted by Stripe, so no card data ever touches this service and the
 * existing {@code redirectUrl} abstraction holds: {@code paymentRef} is the Checkout Session id and
 * {@link #confirm} retrieves the session and reads its {@code payment_status}.
 */
@Component
@ConditionalOnProperty(name = "kdg.payment.provider", havingValue = "stripe")
public class StripePaymentGateway implements PaymentGateway {

    private static final String ORDER_ID_PLACEHOLDER = "{orderId}";
    private static final String PAID_STATUS = "paid";

    private final PaymentProperties properties;
    private final StripeClient client;

    public StripePaymentGateway(PaymentProperties properties, StripeClient client) {
        this.properties = properties;
        this.client = client;
    }

    @Override
    public StartPaymentResponse startPayment(StartPaymentRequest request) {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(properties.stripe().successUrl()
                        .replace(ORDER_ID_PLACEHOLDER, request.orderId()))
                .setCancelUrl(properties.stripe().cancelUrl())
                .setClientReferenceId(request.orderId())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(request.currency().toLowerCase())
                                .setUnitAmount(toCents(request.amount()))
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Order " + request.orderId())
                                        .build())
                                .build())
                        .build())
                .build();

        Session session;
        try {
            session = client.checkout().sessions().create(params);
        } catch (StripeException e) {
            throw new IllegalStateException("Stripe Checkout Session creation failed", e);
        }
        return new StartPaymentResponse(session.getId(), session.getUrl());
    }

    @Override
    public PaymentConfirmation confirm(String paymentRef) {
        Session session;
        try {
            session = client.checkout().sessions().retrieve(paymentRef);
        } catch (StripeException e) {
            throw new IllegalStateException("Stripe Checkout Session retrieval failed", e);
        }
        boolean paid = PAID_STATUS.equals(session.getPaymentStatus());
        return new PaymentConfirmation(paymentRef, paid
                ? PaymentConfirmation.PaymentStatus.PAID
                : PaymentConfirmation.PaymentStatus.FAILED);
    }

    /** Convert euro amount to Stripe's smallest unit (cents) without floating-point drift. */
    private static long toCents(double amount) {
        return BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(100)).longValue();
    }
}
