package be.kdg.backend.api;

import be.kdg.backend.domain.delivery.DeliveryOwnershipException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Maps every domain/infra exception to the right HTTP status (mistake #20 —
 * no try/catch in controllers; advice owns error handling).
 */
class DeliveryExceptionHandlerTest {

    private final DeliveryExceptionHandler handler = new DeliveryExceptionHandler();

    @Test
    void ownershipViolationIsForbidden() {
        ResponseEntity<Map<String, Object>> resp =
                handler.notYourDelivery(new DeliveryOwnershipException("not your delivery"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).containsEntry("code", "NOT_ASSIGNED_COURIER");
    }

    @Test
    void illegalStateIsConflict() {
        var resp = handler.illegalState(new IllegalStateException("bad state"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat((Integer) resp.getBody().get("status")).isEqualTo(409);
    }

    @Test
    void illegalArgumentIsBadRequest() {
        var resp = handler.badArg(new IllegalArgumentException("unsupported status 'NOPE'"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).containsEntry("message", "unsupported status 'NOPE'");
    }

    @Test
    void notFoundIsNotFoundWithBody() {
        var resp = handler.notFound(
                new be.kdg.backend.domain.delivery.DeliveryNotFoundException(be.kdg.backend.domain.shared.DeliveryId.generate()));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody().get("timestamp")).isNotNull();
    }
}
