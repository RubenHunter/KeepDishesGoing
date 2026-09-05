package be.kdg.backend.domain.delivery;

public enum DeliveryStatus {
    PENDING,
    ASSIGNED,
    READY_FOR_PICKUP,
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED
}