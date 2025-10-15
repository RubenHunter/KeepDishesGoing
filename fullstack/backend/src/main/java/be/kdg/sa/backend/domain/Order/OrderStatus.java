package be.kdg.sa.backend.domain.Order;

public enum OrderStatus {
    PENDING,
    PLACED,
    ACCEPTED,
    REJECTED,
    READY_FOR_PICKUP,
    PICKED_UP,
    DELIVERED,
    CANCELLED
}