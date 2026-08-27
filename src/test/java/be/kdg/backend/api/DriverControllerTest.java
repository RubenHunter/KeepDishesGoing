package be.kdg.backend.api;

import be.kdg.backend.application.DeliveryPersonService;
import be.kdg.backend.application.PayoutService;
import be.kdg.backend.domain.payout.Payout;
import be.kdg.backend.domain.shared.DeliveryId;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import be.kdg.backend.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Direct unit tests for driver console endpoints (US35) — registration derives
 * the driver id from the JWT subject, payouts list keeps the running total.
 */
@ExtendWith(MockitoExtension.class)
class DriverControllerTest {

    @Mock DeliveryPersonService driverService;
    @Mock PayoutService payoutService;

    private DriverController controller;
    private UUID subject;

    @BeforeEach
    void setUp() {
        controller = new DriverController(driverService, payoutService);
        subject = UUID.randomUUID();
    }

    private JwtAuthenticationToken jwt(String sub) {
        Jwt token = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(sub)
                .build();
        return new JwtAuthenticationToken(token);
    }

    @Test
    void registerDerivesDriverIdFromJwtSubject() {
        ResponseEntity<DriverController.DriverResponse> resp =
                controller.register(new DriverController.RegisterDriverRequest("Ruben", "BICYCLE"), jwt(subject.toString()));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(driverService).registerDriverIfAbsent(
                org.mockito.ArgumentMatchers.eq(DeliveryPersonId.of(subject)),
                org.mockito.ArgumentMatchers.eq("Ruben"),
                org.mockito.ArgumentMatchers.eq("BICYCLE"));
        assertThat(resp.getBody().driverId()).isEqualTo(subject);
    }

    /** Minimal policy fixture (same approach as PayoutPolicyTest). */
    static final class FixedPayoutPolicy implements be.kdg.backend.domain.payout.PayoutPolicy {
        public Money baseFee() { return Money.ofEuros(1.50); }
        public Money perMinuteFee() { return Money.ofEuros(0.45); }
        public int minMinutes() { return 5; }
        public int maxMinutes() { return 60; }
    }

    @Test
    void payoutsListReturnsRowsAndRunningTotal() {
        Payout p = Payout.compute(DeliveryId.generate(), DeliveryPersonId.of(subject),
                LocalDateTime.now().minusMinutes(30), LocalDateTime.now().minusMinutes(10),
                new FixedPayoutPolicy());
        var summary = new PayoutService.DriverPayoutSummary(DeliveryPersonId.of(subject), List.of(p),
                p.total());
        given(payoutService.summary(DeliveryPersonId.of(subject))).willReturn(summary);

        ResponseEntity<DriverController.DriverPayoutsResponse> resp =
                controller.payouts(subject);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().rows()).hasSize(1);
        assertThat(resp.getBody().totalAmount()).isEqualByComparingTo(p.total().amount());
    }
}
