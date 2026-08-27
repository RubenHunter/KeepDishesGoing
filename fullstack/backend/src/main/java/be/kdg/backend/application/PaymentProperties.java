package be.kdg.backend.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Payment provider + webhook config (US20). The webhook secret + header name secure the
 * confirm endpoint (T3): a caller must present the shared secret in the configured header.
 */
@ConfigurationProperties(prefix = "kdg.payment")
public record PaymentProperties(
        String webhookSecret,
        String webhookSecretHeader,
        Stub stub
) {
    /**
     * Stub provider dev config. {@code redirectTemplate} may contain a {@code {orderId}} placeholder
     * resolved at runtime; it points the browser at the frontend tracking page after (stub) payment.
     */
    public record Stub(String redirectTemplate) {}
}
