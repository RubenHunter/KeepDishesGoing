package be.kdg.backend.infrastructure.persistence.delivery;

import be.kdg.backend.domain.delivery.Delivery;
import be.kdg.backend.domain.delivery.DeliveryStatus;
import be.kdg.backend.domain.shared.Address;
import be.kdg.backend.domain.shared.DeliveryId;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import be.kdg.backend.domain.shared.OrderId;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deliveries", schema = "delivery")
public class JpaDeliveryEntity {

    @Id
    private UUID id;
    private UUID orderId;
    private String pickupStreet;
    private String pickupNumber;
    private String pickupPostalCode;
    private String pickupCity;
    private String pickupCountry;
    private String deliveryStreet;
    private String deliveryNumber;
    private String deliveryPostalCode;
    private String deliveryCity;
    private String deliveryCountry;

    private UUID deliveryPersonId;
    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;
    private boolean availableForSelfAssignment;
    private LocalDateTime assignedAt;
    private LocalDateTime readyAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime inTransitAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;

    public JpaDeliveryEntity() {}

    public static JpaDeliveryEntity from(Delivery d) {
        JpaDeliveryEntity e = new JpaDeliveryEntity();
        e.id = d.id().value();
        e.orderId = d.orderId().value();
        e.pickupStreet = d.pickupAddress().street();
        e.pickupNumber = d.pickupAddress().number();
        e.pickupPostalCode = d.pickupAddress().postalCode();
        e.pickupCity = d.pickupAddress().city();
        e.pickupCountry = d.pickupAddress().country();
        e.deliveryStreet = d.deliveryAddress().street();
        e.deliveryNumber = d.deliveryAddress().number();
        e.deliveryPostalCode = d.deliveryAddress().postalCode();
        e.deliveryCity = d.deliveryAddress().city();
        e.deliveryCountry = d.deliveryAddress().country();
        e.deliveryPersonId = d.deliveryPersonId() == null ? null : d.deliveryPersonId().value();
        e.status = d.status();
        e.availableForSelfAssignment = e.deliveryPersonId == null; // recompute fresh on save
        e.assignedAt = d.assignedAt();
        e.readyAt = d.readyAt();
        e.pickedUpAt = d.pickedUpAt();
        e.inTransitAt = d.inTransitAt();
        e.deliveredAt = d.deliveredAt();
        e.cancelledAt = d.cancelledAt();
        e.cancellationReason = d.cancellationReason();
        return e;
    }

    public Delivery toDomain() {
        Address pickup = new Address(pickupStreet, pickupNumber, pickupPostalCode, pickupCity, pickupCountry);
        Address delivery = new Address(deliveryStreet, deliveryNumber, deliveryPostalCode, deliveryCity, deliveryCountry);
        return Delivery.rehydrate(
                DeliveryId.of(id),
                OrderId.of(orderId),
                pickup,
                delivery,
                deliveryPersonId == null ? null : DeliveryPersonId.of(deliveryPersonId),
                status,
                availableForSelfAssignment,
                assignedAt, readyAt, pickedUpAt, inTransitAt, deliveredAt, cancelledAt, cancellationReason
        );
    }
}