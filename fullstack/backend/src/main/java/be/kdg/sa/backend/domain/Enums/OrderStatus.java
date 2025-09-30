package be.kdg.sa.backend.domain.Enums;

public enum OrderStatus {
    PENDING,        // Order being created, items can be modified
    PLACED,         // Order submitted by customer, waiting for restaurant
    ACCEPTED,       // Restaurant accepted the order
    REJECTED,       // Restaurant rejected the order
    PREPARING,      // Restaurant is preparing the order
    READY_FOR_PICKUP, // Order is ready for pickup/delivery
    PICKED_UP,      // Courier has picked up the order
    DELIVERED,      // Order has been delivered
    CANCELLED       // Order was cancelled
}
