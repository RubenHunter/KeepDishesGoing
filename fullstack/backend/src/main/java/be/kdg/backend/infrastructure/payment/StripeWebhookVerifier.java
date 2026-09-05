package be.kdg.backend.infrastructure.payment;

import be.kdg.backend.application.PaymentProperties;
import be.kdg.backend.domain.StripeSignatureException;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.springframework.stereotype.Component;

/**
 * Verifies Stripe webhook payloads and extracts the Checkout Session id for completed checkouts.
 * Isolates the Stripe SDK (checked {@link SignatureVerificationException}) behind a domain boundary:
 * the controller never imports Stripe types and never uses try/catch (coding-mistakes #7, #20).
 */
@Component
public class StripeWebhookVerifier {

    private static final String CHECKOUT_COMPLETED = "checkout.session.completed";

    private final PaymentProperties properties;

    public StripeWebhookVerifier(PaymentProperties properties) {
        this.properties = properties;
    }

    /**
     * @return the Checkout Session id when the payload is a {@code checkout.session.completed} event,
     *         or {@code null} for any other event type (no-op).
     * @throws StripeSignatureException when the {@code Stripe-Signature} header does not verify.
     */
    public String completedCheckoutSessionId(String payload, String signatureHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, properties.stripe().webhookSecret());
        } catch (SignatureVerificationException e) {
            throw new StripeSignatureException("Invalid Stripe webhook signature");
        }
        if (!CHECKOUT_COMPLETED.equals(event.getType())) {
            return null;
        }
        Session session = checkoutSession(event.getDataObjectDeserializer());
        return session == null ? null : session.getId();
    }

    /**
     * Extract the Checkout Session. {@link EventDataObjectDeserializer#getObject()} refuses to
     * deserialize when the event's API version differs from the SDK's pinned version (always the
     * case with {@code stripe listen}), so fall back to {@code deserializeUnsafe()} — the session
     * {@code id} field is stable across API versions.
     */
    private Session checkoutSession(EventDataObjectDeserializer deserializer) {
        StripeObject object = deserializer.getObject().orElse(null);
        if (object == null) {
            try {
                object = deserializer.deserializeUnsafe();
            } catch (EventDataObjectDeserializationException e) {
                throw new StripeSignatureException("Unable to deserialize Stripe webhook payload");
            }
        }
        return object instanceof Session session ? session : null;
    }
}
