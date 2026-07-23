package be.kdg.backend.api;

import be.kdg.backend.application.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * US20 payment webhook endpoint (stub).
 *
 * Real providers (Mollie/Stripe) would POST here after a payment session is completed.
 * In the stub PaymentGateway, the redirect URL points the browser here as a GET, so we accept both.
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final OrderService orderService;

    @PostMapping("/{paymentRef}/confirm")
    public ResponseEntity<Void> confirmPost(@PathVariable String paymentRef) {
        orderService.confirmPayment(paymentRef);
        return ResponseEntity.ok().build();
    }

    /** Convenience GET so the browser can land on the stub redirect and POST back. */
    @GetMapping("/{paymentRef}/confirm")
    public ResponseEntity<Void> confirmGet(@PathVariable String paymentRef) {
        orderService.confirmPayment(paymentRef);
        return ResponseEntity.ok().build();
    }
}