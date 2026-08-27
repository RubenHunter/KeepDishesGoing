package be.kdg.backend.api;

import be.kdg.backend.api.dto.DeliveryResponse;
import be.kdg.backend.application.DeliveryService;
import be.kdg.backend.domain.delivery.Delivery;
import be.kdg.backend.domain.payout.PayoutPolicy;
import be.kdg.backend.domain.shared.DeliveryId;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Courier-flows REST endpoints. JWT required (driver role).
 * The caller identity is the JWT subject — never the request body/query params.
 * US29/US30: only the assigned courier may cancel/pickup/transit/deliver their delivery.
 *
 * Lifecycle transitions use resource-style PATCH /{id}/status (mistake #16).
 */
@Slf4j
@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final PayoutPolicy payoutPolicy;

    /**
     * Resource-style lifecycle update. Supported:
     * ASSIGNED (= self-assign/claim), CANCELLED (= release claim), PICKED_UP,
     * IN_TRANSIT, DELIVERED.
     */
    @PatchMapping("/{deliveryId}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable UUID deliveryId,
                                             JwtAuthenticationToken jwt,
                                             @RequestBody DeliveryStatusUpdate body) {
        String status = body == null || body.status() == null ? "" : body.status().toUpperCase(Locale.ROOT);
        return switch (status) {
            case "ASSIGNED" -> {
                deliveryService.selfAssignDelivery(DeliveryId.of(deliveryId), driver(jwt), LocalDateTime.now());
                yield ResponseEntity.ok().build();
            }
            case "CANCELLED" -> {
                String reason = body.reason() == null ? "Cancelled by courier" : body.reason();
                deliveryService.cancelClaim(DeliveryId.of(deliveryId), driver(jwt), reason, LocalDateTime.now());
                yield ResponseEntity.noContent().build();
            }
            case "PICKED_UP" -> {
                deliveryService.markPickedUp(DeliveryId.of(deliveryId), driver(jwt), LocalDateTime.now());
                yield ResponseEntity.ok().build();
            }
            case "IN_TRANSIT" -> {
                deliveryService.markInTransit(DeliveryId.of(deliveryId), driver(jwt), LocalDateTime.now());
                yield ResponseEntity.ok().build();
            }
            case "DELIVERED" -> {
                deliveryService.markDelivered(DeliveryId.of(deliveryId), driver(jwt), LocalDateTime.now());
                yield ResponseEntity.ok().build();
            }
            default -> throw new IllegalArgumentException(
                    "Field 'status' must be one of ASSIGNED|CANCELLED|PICKED_UP|IN_TRANSIT|DELIVERED");
        };
    }


    // ---- Read endpoints ----

    /** US28 — available-to-claim list. */
    @GetMapping("/available")
    public ResponseEntity<List<DeliveryResponse>> available() {
        return ResponseEntity.ok(deliveryService.listAvailable().stream()
                .map(d -> DeliveryResponse.from(d, payoutPolicy))
                .toList());
    }

    @GetMapping("/{deliveryId}")
    public ResponseEntity<DeliveryResponse> get(@PathVariable UUID deliveryId) {
        return ResponseEntity.ok(DeliveryResponse.from(
                deliveryService.get(DeliveryId.of(deliveryId)), payoutPolicy));
    }

    @GetMapping
    public ResponseEntity<List<DeliveryResponse>> forDriver(JwtAuthenticationToken jwt) {
        return ResponseEntity.ok(
                deliveryService.listForDriver(driver(jwt)).stream()
                        .map(d -> DeliveryResponse.from(d, payoutPolicy))
                        .toList());
    }

    public record CancelClaimRequest(String reason) {}

    /** The caller identity — always the JWT subject, never a body/param. */
    private static DeliveryPersonId driver(JwtAuthenticationToken jwt) {
        return DeliveryPersonId.of(jwt.getToken().getSubject());
    }

    /** Body of PATCH /deliveries/{id}/status — resource-style lifecycle update. */
    public record DeliveryStatusUpdate(String status, String reason) {}
}
