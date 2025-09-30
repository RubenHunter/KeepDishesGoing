package be.kdg.sa.backend.application;

public record UpdateOrderStatusCommand(
        String orderId,
        String status,
        String reason
) {
    public UpdateOrderStatusCommand {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }
    }
}

