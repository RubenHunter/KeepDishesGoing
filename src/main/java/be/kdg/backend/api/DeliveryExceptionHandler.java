package be.kdg.backend.api;

import be.kdg.backend.domain.delivery.DeliveryAlreadyAssignedException;
import be.kdg.backend.domain.delivery.DeliveryNotFoundException;
import be.kdg.backend.domain.driver.DeliveryPersonAlreadyAssignedException;
import be.kdg.backend.domain.driver.DeliveryPersonNotFoundException;
import be.kdg.backend.domain.shared.Money;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

/** Mirrors order-service exception handler — bad inputs WARN, infra errors ERROR. */
@Slf4j
@RestControllerAdvice
public class DeliveryExceptionHandler {

    @ExceptionHandler(DeliveryNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(DeliveryNotFoundException ex) {
        log.warn("notFound: {}", ex.getMessage());
        return status(HttpStatus.NOT_FOUND, ex.getMessage(), "NOT_FOUND");
    }
    @ExceptionHandler(DeliveryPersonNotFoundException.class)
    public ResponseEntity<Map<String, Object>> driverNotFound(DeliveryPersonNotFoundException ex) {
        log.warn("driverNotFound: {}", ex.getMessage());
        return status(HttpStatus.NOT_FOUND, ex.getMessage(), "DRIVER_NOT_FOUND");
    }
    @ExceptionHandler(DeliveryAlreadyAssignedException.class)
    public ResponseEntity<Map<String, Object>> alreadyAssigned(DeliveryAlreadyAssignedException ex) {
        log.warn("alreadyAssigned: {}", ex.getMessage());
        return status(HttpStatus.CONFLICT, ex.getMessage(), "DELIVERY_ASSIGNED");
    }
    @ExceptionHandler(DeliveryPersonAlreadyAssignedException.class)
    public ResponseEntity<Map<String, Object>> driverBusy(DeliveryPersonAlreadyAssignedException ex) {
        log.warn("driverBusy: {}", ex.getMessage());
        return status(HttpStatus.CONFLICT, ex.getMessage(), "DRIVER_BUSY");
    }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> illegalState(IllegalStateException ex) {
        log.warn("illegalState: {}", ex.getMessage());
        return status(HttpStatus.CONFLICT, ex.getMessage(), "ILLEGAL_STATE");
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badArg(IllegalArgumentException ex) {
        log.warn("badArg: {}", ex.getMessage());
        return status(HttpStatus.BAD_REQUEST, ex.getMessage(), "ILLEGAL_ARGUMENT");
    }

    private ResponseEntity<Map<String, Object>> status(HttpStatus s, String message, String code) {
        return ResponseEntity.status(s).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", s.value(),
                "error", s.getReasonPhrase(),
                "code", code,
                "message", message == null ? "" : message
        ));
    }
}