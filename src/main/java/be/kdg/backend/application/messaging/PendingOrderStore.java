package be.kdg.backend.application.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store of pending order delivery addresses received from
 * {@code order.placed} events. Looked up by {@code OrderEventController}
 * when the owner accepts an order and publishes {@code order.accepted}.
 * <p>
 * Trade-off: in-memory is simpler than adding a DB table. Acceptable because
 * restaurant-service uses {@code ddl-auto=create} (data is ephemeral anyway)
 * and the window between PLACED and ACCEPTED is short (5 min per US23/US24).
 */
@Slf4j
@Component
public class PendingOrderStore {

    private final Map<String, String> deliveryAddressByOrderId = new ConcurrentHashMap<>();

    public void put(String orderId, String deliveryAddress) {
        deliveryAddressByOrderId.put(orderId, deliveryAddress);
        log.debug("Stored order {} delivery address", orderId);
    }

    public String get(String orderId) {
        return deliveryAddressByOrderId.get(orderId);
    }

    public void remove(String orderId) {
        deliveryAddressByOrderId.remove(orderId);
        log.debug("Removed order {} from store", orderId);
    }
}
