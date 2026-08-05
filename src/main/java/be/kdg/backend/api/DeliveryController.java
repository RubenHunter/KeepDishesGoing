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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Courier-flows REST endpoints. JWT required (driver role).
 */
@Slf4j
@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final PayoutPolicy payoutPolicy;

    /** US27 — courier claims a delivery (self-assign). */
    @PostMapping("/{deliveryId}/claim")
    public ResponseEntity<Void> claim(@PathVariable UUID deliveryId, @RequestBody SelfClaimRequest req) {
        deliveryService.selfAssignDelivery(DeliveryId.of(deliveryId), DeliveryPersonId.of(req.driverId()), LocalDateTime.now());
        return ResponseEntity.ok().build();
    }

    /** US29 — courier releases the claim (only valid while order not yet READY). */
    @PostMapping("/{deliveryId}/cancel-claim")
    public ResponseEntity<Void> cancelClaim(@PathVariable UUID deliveryId, @RequestBody CancelClaimRequest req) {
        deliveryService.cancelClaim(DeliveryId.of(deliveryId), req.reason(), LocalDateTime.now());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{deliveryId}/pickup")
    public ResponseEntity<Void> pickup(@PathVariable UUID deliveryId) {
        deliveryService.markPickedUp(DeliveryId.of(deliveryId), LocalDateTime.now());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{deliveryId}/transit")
    public ResponseEntity<Void> transit(@PathVariable UUID deliveryId) {
        deliveryService.markInTransit(DeliveryId.of(deliveryId), LocalDateTime.now());
        return ResponseEntity.ok().build();
    }

    /** US30 — final step; triggers Payout calc and OrderDelivered AMQP event. */
    @PostMapping("/{deliveryId}/deliver")
    public ResponseEntity<Void> deliver(@PathVariable UUID deliveryId) {
        deliveryService.markDelivered(DeliveryId.of(deliveryId), LocalDateTime.now());
        return ResponseEntity.ok().build();
    }

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
    public ResponseEntity<List<DeliveryResponse>> forDriver(@RequestParam UUID driverId) {
        return ResponseEntity.ok(
                deliveryService.listForDriver(DeliveryPersonId.of(driverId)).stream()
                        .map(d -> DeliveryResponse.from(d, payoutPolicy))
                        .toList());
    }

    public record SelfClaimRequest(UUID driverId) {}
    public record CancelClaimRequest(String reason) {}
}