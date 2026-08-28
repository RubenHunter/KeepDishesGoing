package be.kdg.backend.infrastructure.payment;

import be.kdg.backend.application.PaymentProperties;
import com.stripe.StripeClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the Stripe SDK client when the stripe provider is active. Kept out of the gateway so the
 * gateway stays unit-testable (a mock {@link StripeClient} can be injected) and the SDK is wired
 * through normal Spring DI rather than a static {@code Stripe.apiKey}.
 */
@Configuration
public class StripeConfig {

    @Bean
    @ConditionalOnProperty(name = "kdg.payment.provider", havingValue = "stripe")
    StripeClient stripeClient(PaymentProperties properties) {
        return new StripeClient(properties.stripe().secretKey());
    }
}
