package be.kdg.backend.application;

import be.kdg.backend.application.messaging.InternalEvents;
import be.kdg.backend.application.messaging.OutboundEventPublisher;
import be.kdg.backend.domain.delivery.Delivery;
import be.kdg.backend.domain.delivery.DeliveryRepository;
import be.kdg.backend.domain.shared.Address;
import be.kdg.backend.domain.shared.DeliveryId;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import be.kdg.backend.domain.shared.OrderId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Delivery application service — does NOT contain domain logic, only orchestration
 * (coding-mistakes #9: no domain logic in services). Each method maps to a use case.
 *
 * Cross-aggregate assignments go via Spring {@link ApplicationEventPublisher} AFTER commit
 * to satisfy one-aggregate-per-tx rule (coding-mistakes #3).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final ApplicationEventPublisher springEvents;
    private final OutboundEventPublisher outbound;

    // ---- Consumers of AMQP events ----

    /** OrderAcceptedHandler — creates PENDING Delivery that becomes self-assignable (US28). */
    @Transactional
    public Delivery onOrderAccepted(OrderId orderId, Address pickupAddress, Address deliveryAddress) {
        log.info("Creating Delivery for order {}", orderId);
        var existing = deliveryRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            log.warn("Delivery for order {} already exists — ignoring duplicate", orderId);
            return existing.get();
        }
        Delivery delivery = new Delivery(DeliveryId.generate(), orderId, pickupAddress, deliveryAddress);
        return deliveryRepository.save(delivery);
    }

    @Transactional
    public void onOrderReadyForPickup(OrderId orderId, LocalDateTime at) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("No delivery for order " + orderId));
        delivery.onOrderReadyForPickup(at);
        deliveryRepository.save(delivery);
    }

    // ---- Customer (driver) flows ----

    @Transactional
    public void selfAssignDelivery(DeliveryId deliveryId, DeliveryPersonId driverId, LocalDateTime now) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new be.kdg.backend.domain.delivery.DeliveryNotFoundException(deliveryId));
        delivery.selfAssign(driverId, now);
        deliveryRepository.save(delivery);

        // Event will fire AFTER_COMMIT — second tx will save DeliveryPerson (one-agg-per-tx).
        springEvents.publishEvent(new InternalEvents.CourierAssignedEvent(deliveryId, driverId, now));
        log.info("Delivery {} claimed by {}, event queued", deliveryId, driverId);
    }

    /**
     * US29 — releases the claim. The requester must be the courier currently assigned
     * to the delivery (US31: one active assignment; nobody else may touch it).
     */
    @Transactional
    public void cancelClaim(DeliveryId deliveryId, DeliveryPersonId requester, String reason, LocalDateTime now) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new be.kdg.backend.domain.delivery.DeliveryNotFoundException(deliveryId));
        requireAssignedCourier(delivery, requester);
        DeliveryPersonId driverId = delivery.deliveryPersonId();
        if (driverId == null) {
            throw new IllegalStateException("No courier assigned to delivery " + deliveryId);
        }
        delivery.cancelClaim(reason, now);
        deliveryRepository.save(delivery);

        springEvents.publishEvent(new InternalEvents.CourierReleasedEvent(driverId, now));
        log.info("Courier {} released from delivery {}", driverId, deliveryId);
    }

    /** Authz guard — only the assigned courier may advance their own delivery. */
    private void requireAssignedCourier(Delivery delivery, DeliveryPersonId requester) {
        DeliveryPersonId assigned = delivery.deliveryPersonId();
        if (assigned == null) {
            throw new be.kdg.backend.domain.delivery.DeliveryOwnershipException(
                    "Delivery " + delivery.id() + " has no assigned courier yet");
        }
        if (!assigned.equals(requester)) {
            log.warn("Driver {} attempted to act on delivery {} owned by {} — denied",
                    requester, delivery.id(), assigned);
            throw new be.kdg.backend.domain.delivery.DeliveryOwnershipException(
                    "Delivery " + delivery.id() + " is not assigned to you");
        }
    }

    @Transactional
    public void markPickedUp(DeliveryId deliveryId, DeliveryPersonId requester, LocalDateTime now) {
        Delivery delivery = find(deliveryId);
        requireAssignedCourier(delivery, requester);
        delivery.markPickedUp(now);
        deliveryRepository.save(delivery);
        outbound.publishPickedUp(delivery.orderId().value(), delivery.id().value(), now);
    }

    @Transactional
    public void markInTransit(DeliveryId deliveryId, DeliveryPersonId requester, LocalDateTime now) {
        Delivery delivery = find(deliveryId);
        requireAssignedCourier(delivery, requester);
        delivery.markInTransit(now);
        deliveryRepository.save(delivery);
    }

    /**
     * markDelivered fulfils three side-effects:
     *  - mutate Delivery AR (tx1)
     *  - publish internal Spring event to compute Payout in a separate tx (one-agg-per-tx)
     *  - publish AMQP OrderDelivered for order-service
     */
    @Transactional
    public void markDelivered(DeliveryId deliveryId, DeliveryPersonId requester, LocalDateTime now) {
        Delivery delivery = find(deliveryId);
        requireAssignedCourier(delivery, requester);
        delivery.markDelivered(now);
        deliveryRepository.save(delivery);
        DeliveryPersonId driver = delivery.deliveryPersonId();
        if (driver != null) {
            springEvents.publishEvent(new InternalEvents.DeliveryDeliveredEvent(
                    delivery.id(), driver, delivery.readyAt(), delivery.deliveredAt()));
            springEvents.publishEvent(new InternalEvents.CourierReleasedEvent(driver, now));
        }
        outbound.publishDelivered(delivery.orderId().value(), delivery.id().value(), now);
    }

    // ---- Read endpoints ----

    public Delivery get(DeliveryId id) {
        return find(id);
    }

    public List<Delivery> listAvailable() {
        return deliveryRepository.findAvailableForSelfAssignment();
    }

    public List<Delivery> listForDriver(DeliveryPersonId driver) {
        return deliveryRepository.findByDeliveryPersonId(driver);
    }

    private Delivery find(DeliveryId id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new be.kdg.backend.domain.delivery.DeliveryNotFoundException(id));
    }
}