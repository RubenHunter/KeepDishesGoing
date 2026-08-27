package be.kdg.backend.api;

import be.kdg.backend.application.DeliveryService;
import be.kdg.backend.domain.payout.PayoutPolicy;
import be.kdg.backend.domain.shared.DeliveryId;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * One endpoint, every lifecycle transition (mistake #16). The JWT subject is the driver id.
 */
@ExtendWith(MockitoExtension.class)
class DeliveryControllerTest {

    @Mock DeliveryService deliveryService;
    @Mock PayoutPolicy payoutPolicy;

    private DeliveryController controller;
    private UUID subject;

    @BeforeEach
    void setUp() {
        controller = new DeliveryController(deliveryService, payoutPolicy);
        subject = UUID.randomUUID();
    }

    private JwtAuthenticationToken jwt() {
        return new JwtAuthenticationToken(Jwt.withTokenValue("t").header("alg", "none")
                .subject(subject.toString()).build());
    }

    private ResponseEntity<Void> patch(String body) {
        // body parsed manually — exercises the same record Spring binds
        String status = null, reason = null;
        if (body.contains("\"status\"")) {
            status = body.split("\"status\":\"")[1].split("\"")[0];
        }
        if (body.contains("\"reason\"")) {
            reason = body.split("\"reason\":\"")[1].split("\"")[0];
        }
        var update = new DeliveryController.DeliveryStatusUpdate(status, reason);
        return controller.updateStatus(UUID.randomUUID(), jwt(), update);
    }

    @Test
    void assignedDelegatesToSelfAssign() {
        assertThat(patch("{\"status\":\"ASSIGNED\"}").getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(deliveryService).selfAssignDelivery(any(DeliveryId.class), any(DeliveryPersonId.class),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deliveredDelegatesWithJwtDriver() {
        assertThat(patch("{\"status\":\"DELIVERED\"}").getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(deliveryService).markDelivered(any(DeliveryId.class),
                org.mockito.ArgumentMatchers.eq(DeliveryPersonId.of(subject)),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void cancelledRequiresNoContentAndUsesReason() {
        assertThat(patch("{\"status\":\"CANCELLED\",\"reason\":\"nope\"}").getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        verify(deliveryService).cancelClaim(any(DeliveryId.class), any(DeliveryPersonId.class),
                org.mockito.ArgumentMatchers.eq("nope"), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unknownStatusIsRejected() {
        assertThatThrownBy(() -> patch("{\"status\":\"YOINK\"}"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(deliveryService, never()).markDelivered(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private static <T> T any(Class<T> type) { return org.mockito.ArgumentMatchers.any(type); }
}
