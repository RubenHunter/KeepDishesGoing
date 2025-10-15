package be.kdg.sa.backend.application;

public class ShoppingCartNotFoundException extends RuntimeException {
    public ShoppingCartNotFoundException(String message) {
        super(message);
    }
}