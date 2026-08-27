package be.kdg.backend.api;

import be.kdg.backend.application.OrderService;
import be.kdg.backend.application.PaymentProperties;
import be.kdg.backend.domain.PaymentSignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * US20 payment webhook endpoint (stub).
 *
 * Real providers (Mollie/Stripe) would POST here after a payment session is completed. The endpoint
 * is left open at the security layer (permitAll) but is guarded by a shared-secret header (T3):
 * a caller must present the configured secret in {@code kdg.payment.webhook-secret-header} or the
 * request is rejected with 403. Confirmation is idempotent — {@link OrderService#confirmPayment} only
 * assigns payment on a still-PENDING order.
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final OrderService orderService;
    private final PaymentProperties paymentProperties;

    @PostMapping("/{paymentRef}/confirm")
    public ResponseEntity<Void> confirmPost(@PathVariable String paymentRef, HttpServletRequest request) {
        String signature = request.getHeader(paymentProperties.webhookSecretHeader());
        if (!paymentProperties.webhookSecret().equals(signature)) {
            log.warn("Payment webhook rejected — missing/invalid signature header");
            throw new PaymentSignatureException("Invalid payment webhook signature");
        }
        orderService.confirmPayment(paymentRef);
        return ResponseEntity.ok().build();
    }
}
