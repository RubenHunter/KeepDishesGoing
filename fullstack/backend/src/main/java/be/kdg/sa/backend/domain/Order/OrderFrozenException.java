package be.kdg.sa.backend.domain.Order;

public class OrderFrozenException extends RuntimeException {
    public OrderFrozenException(String message) {
        super(message);
    }
}