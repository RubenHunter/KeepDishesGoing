package be.kdg.backend.api;

import be.kdg.backend.domain.DomainException;
import be.kdg.backend.domain.NotFoundException;
import be.kdg.backend.domain.PaymentSignatureException;
import be.kdg.backend.domain.ValidationException;
import be.kdg.backend.domain.order.OrderFrozenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Global exception handler — mirrors restaurant-service {@code ErrorHandling} shape.
 * No try/catch in controllers; throw → translate here.
 */
@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(NotFoundException ex) {
        log.warn("NotFoundException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body(HttpStatus.NOT_FOUND, ex.getMessage(), "NOT_FOUND"));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> validation(ValidationException ex) {
        log.warn("ValidationException: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST, ex.getMessage(), "VALIDATION"));
    }

    @ExceptionHandler(OrderFrozenException.class)
    public ResponseEntity<Map<String, Object>> frozen(OrderFrozenException ex) {
        log.warn("OrderFrozenException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(HttpStatus.CONFLICT, ex.getMessage(), "ORDER_FROZEN"));
    }

    @ExceptionHandler(PaymentSignatureException.class)
    public ResponseEntity<Map<String, Object>> paymentSignature(PaymentSignatureException ex) {
        log.warn("PaymentSignatureException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body(HttpStatus.FORBIDDEN, ex.getMessage(), "INVALID_SIGNATURE"));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, Object>> domain(DomainException ex) {
        log.warn("DomainException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(HttpStatus.CONFLICT, ex.getMessage(), "DOMAIN"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> illegalArg(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST, ex.getMessage(), "ILLEGAL_ARGUMENT"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> illegalState(IllegalStateException ex) {
        log.warn("IllegalStateException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(HttpStatus.CONFLICT, ex.getMessage(), "ILLEGAL_STATE"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> notValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "; " + b);
        log.warn("MethodArgumentNotValidException: {}", msg);
        return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST, msg, "VALIDATION"));
    }

    private Map<String, Object> body(HttpStatus status, String message, String code) {
        return Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "code", code,
                "message", message == null ? "" : message
        );
    }
}