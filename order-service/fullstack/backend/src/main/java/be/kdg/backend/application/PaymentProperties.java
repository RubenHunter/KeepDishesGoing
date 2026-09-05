package be.kdg.backend.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Payment provider + webhook config (US20). The webhook secret + header name secure the
 * confirm endpoint (T3): a caller must present the shared secret in the configured header.
 */
@ConfigurationProperties(prefix = "kdg.payment")
public record PaymentProperties(
        String provider,
        String webhookSecret,
        String webhookSecretHeader,
        Stripe stripe
) {
    public PaymentProperties {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException("kdg.payment.webhook-secret must not be blank");
        }
        if (webhookSecretHeader == null || webhookSecretHeader.isBlank()) {
            throw new IllegalStateException("kdg.payment.webhook-secret-header must not be blank");
        }
    }

    /**
     * Stripe Checkout Session (hosted) provider config. Test-mode keys only; supplied via env vars,
     * never committed. {@code successUrl} may contain an {@code {orderId}} placeholder resolved at
     * runtime — Stripe redirects the browser there after a completed session.
     */
    public record Stripe(String secretKey, String webhookSecret,
                         String successUrl, String cancelUrl) {}
}
