package be.kdg.backend.domain.order;

import java.time.LocalDateTime;

/**
 * Order lifecycle states.
 *
 * Transitions (owned by {@link Order} aggregate):
 *  PENDING → PLACED → ACCEPTED → READY_FOR_PICKUP → PICKED_UP → DELIVERED
 *  PLACED|ACCEPTED → CANCELLED
 *  PLACED → REJECTED (via event from restaurant-service)
 *
 * All terminal states: REJECTED, CANCELLED, DELIVERED.
 */
public enum OrderStatus {
    PENDING,
    PLACED,
    ACCEPTED,
    READY_FOR_PICKUP,
    PICKED_UP,
    DELIVERED,
    REJECTED,
    CANCELLED;

    public boolean isTerminal() {
        return this == DELIVERED || this == REJECTED || this == CANCELLED;
    }

    public boolean canCancelFrom() {
        return this == PENDING || this == PLACED || this == ACCEPTED;
    }
}