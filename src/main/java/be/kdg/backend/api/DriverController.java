package be.kdg.backend.api;

import be.kdg.backend.application.DeliveryPersonService;
import be.kdg.backend.application.PayoutService;
import be.kdg.backend.domain.payout.Payout;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import be.kdg.backend.domain.shared.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Driver endpoints: register + payouts history (US35).
 */
@Slf4j
@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DeliveryPersonService driverService;
    private final PayoutService payoutService;

    /** Register (idempotent) — driver id is always the Keycloak subject, so payouts guard passes. */
    @PostMapping
    public ResponseEntity<DriverResponse> register(@RequestBody RegisterDriverRequest req, JwtAuthenticationToken jwt) {
        var id = DeliveryPersonId.of(UUID.fromString(jwt.getToken().getSubject()));
        String email = jwt.getToken().getClaimAsString("email");
        driverService.registerDriverIfAbsent(id, req.name(), email, req.vehicle());
        return ResponseEntity.ok(new DriverResponse(id.value(), req.name(), req.vehicle(), true));
    }

    /** US35 — list deliveries + payouts + running total for the calling driver. */
    @GetMapping("/{driverId}/payouts")
    @PreAuthorize("@driverGuard.canAccessPayouts(#driverId)")
    public ResponseEntity<DriverPayoutsResponse> payouts(@PathVariable UUID driverId) {
        var summary = payoutService.summary(DeliveryPersonId.of(driverId));
        return ResponseEntity.ok(DriverPayoutsResponse.from(summary));
    }

    public record RegisterDriverRequest(String name, String vehicle) {}
    public record DriverResponse(UUID driverId, String name, String vehicle, boolean available) {}

    public record DriverPayoutsResponse(
            UUID driverId,
            List<PayoutRow> rows,
            String totalCurrency,
            BigDecimal totalAmount
    ) {
        public static DriverPayoutsResponse from(PayoutService.DriverPayoutSummary s) {
            return new DriverPayoutsResponse(
                    s.driverId().value(),
                    s.rows().stream().map(PayoutRow::from).toList(),
                    s.total().currency(),
                    s.total().amount()
            );
        }
        public record PayoutRow(
                UUID payoutId,
                UUID deliveryId,
                int billableMinutes,
                String currency,
                BigDecimal amount,
                LocalDateTime readyAt,
                LocalDateTime deliveredAt,
                LocalDateTime computedAt
        ) {
            public static PayoutRow from(Payout p) {
                return new PayoutRow(
                        p.id().value(),
                        p.deliveryId().value(),
                        p.billableMinutes(),
                        p.total().currency(),
                        p.total().amount(),
                        p.readyAt(),
                        p.deliveredAt(),
                        p.computedAt()
                );
            }
        }
    }
}