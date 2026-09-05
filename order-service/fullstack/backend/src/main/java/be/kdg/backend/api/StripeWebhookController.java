package be.kdg.backend.api;

import be.kdg.backend.application.OrderService;
import be.kdg.backend.infrastructure.payment.StripeWebhookVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stripe webhook entry point (US20, test mode). Stripe POSTs {@code checkout.session.completed}
 * here; the call is authenticated by the {@code Stripe-Signature} header (verified against the
 * configured webhook secret), not the JWT — the path is {@code permitAll} at the security layer.
 * Signature failures surface as {@link be.kdg.backend.domain.StripeSignatureException} (400) via the
 * global advice; no try/catch in the controller (coding-mistakes #20).
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final OrderService orderService;
    private final StripeWebhookVerifier verifier;

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String payload,
                                        @RequestHeader("Stripe-Signature") String signatureHeader) {
        String sessionId = verifier.completedCheckoutSessionId(payload, signatureHeader);
        if (sessionId != null) {
            log.info("Stripe webhook checkout.session.completed session={}", sessionId);
            orderService.confirmPayment(sessionId);
        }
        return ResponseEntity.ok().build();
    }
}
