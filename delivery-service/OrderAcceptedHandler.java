package be.kdg.backend.application.messaging.handlers;

import be.kdg.backend.application.DeliveryService;
import be.kdg.backend.application.messaging.InboundEvents;
import be.kdg.backend.domain.shared.Address;
import be.kdg.backend.domain.shared.OrderId;
import be.kdg.backend.infrastructure.messaging.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Consumes {@code order.accepted} (publisher: restaurant-service). Creates Delivery AR. */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAcceptedHandler {

    private final DeliveryService deliveryService;

    @RabbitListener(queues = RabbitConfig.Q_DELIVERY_ORDER_ACCEPTED)
    public void handle(InboundEvents.OrderAcceptedEvent event) {
        log.info("Consumed OrderAccepted orderId={}", event.orderId());
        // Best-effort parse addresses as single-line strings into Address VO.
        Address pickup = parseAddress(event.pickupAddress());
        Address delivery = parseAddress(event.deliveryAddress());
        try {
            deliveryService.onOrderAccepted(OrderId.of(event.orderId()), pickup, delivery);
        } catch (Exception ex) {
            log.warn("Delivery creation for order {} failed: {}", event.orderId(), ex.getMessage());
        }
    }

    /** Best-effort: split "street number, postal city, country" → Address VO. Falls back to placeholder fields. */
    private Address parseAddress(String singleLine) {
        if (singleLine == null || singleLine.isBlank()) {
            return new Address("Unknown pickup address", "-", "-", "-", "-");
        }
        // Splits like "Langestraat 12, 2000 Antwerpen, BE"
        try {
            String[] parts = singleLine.split(",");
            String[] streetNumber = parts[0].trim().split(" ", 2);
            String street = streetNumber[0];
            String number = streetNumber.length > 1 ? streetNumber[1] : "-";
            String[] pcCity = parts.length > 1 ? parts[1].trim().split(" ", 2) : new String[]{"-", "-"};
            String pc = pcCity[0];
            String city = pcCity.length > 1 ? pcCity[1] : "-";
            String country = parts.length > 2 ? parts[2].trim() : "-";
            return new Address(street, number, pc, city, country);
        } catch (Exception e) {
            return new Address(singleLine, "-", "-", "-", "-");
        }
    }
}