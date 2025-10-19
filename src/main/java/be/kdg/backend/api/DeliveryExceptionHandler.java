package be.kdg.backend.api;
import be.kdg.backend.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class DeliveryExceptionHandler {

    @ExceptionHandler(DeliveryNotAvailableException.class)
    public ResponseEntity<Object> handleDeliveryNotAvailable(DeliveryNotAvailableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "DELIVERY_NOT_AVAILABLE",
                        "message", ex.getMessage(),
                        "code", "US27_VIOLATION"
                ));
    }

    @ExceptionHandler(DeliveryAlreadyAssignedException.class)
    public ResponseEntity<Object> handleDeliveryAlreadyAssigned(DeliveryAlreadyAssignedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "DELIVERY_ALREADY_ASSIGNED",
                        "message", ex.getMessage(),
                        "code", "US32_VIOLATION"
                ));
    }

    @ExceptionHandler(DeliveryPersonAlreadyAssignedException.class)
    public ResponseEntity<Object> handleDeliveryPersonAlreadyAssigned(DeliveryPersonAlreadyAssignedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "DELIVERY_PERSON_ALREADY_ASSIGNED",
                        "message", ex.getMessage(),
                        "code", "US31_VIOLATION"
                ));
    }

    @ExceptionHandler(DeliveryNotFoundException.class)
    public ResponseEntity<Object> handleDeliveryNotFound(DeliveryNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "DELIVERY_NOT_FOUND", "message", ex.getMessage()));
    }

    @ExceptionHandler(DeliveryPersonNotFoundException.class)
    public ResponseEntity<Object> handleDeliveryPersonNotFound(DeliveryPersonNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "DELIVERY_PERSON_NOT_FOUND", "message", ex.getMessage()));
    }

    @ExceptionHandler(InvalidDeliveryStatusException.class)
    public ResponseEntity<Object> handleInvalidDeliveryStatus(InvalidDeliveryStatusException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "INVALID_DELIVERY_STATUS", "message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "INVALID_REQUEST", "message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "INVALID_OPERATION", "message", ex.getMessage()));
    }
}