package be.kdg.backend.infrastructure;

import be.kdg.backend.domain.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "deliveries", schema = "delivery")
@Getter
@NoArgsConstructor
public class DeliveryJpaEntity {
    @Id
    private String id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "delivery_person_id")
    private String deliveryPersonId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "pickup_street")),
            @AttributeOverride(name = "city", column = @Column(name = "pickup_city")),
            @AttributeOverride(name = "postalCode", column = @Column(name = "pickup_postal_code")),
            @AttributeOverride(name = "country", column = @Column(name = "pickup_country"))
    })
    private Address pickupAddress;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "delivery_street")),
            @AttributeOverride(name = "city", column = @Column(name = "delivery_city")),
            @AttributeOverride(name = "postalCode", column = @Column(name = "delivery_postal_code")),
            @AttributeOverride(name = "country", column = @Column(name = "delivery_country"))
    })
    private Address deliveryAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @Column(name = "estimated_delivery_time")
    private LocalDateTime estimatedDeliveryTime;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static DeliveryJpaEntity fromDomain(Delivery delivery) {
        DeliveryJpaEntity entity = new DeliveryJpaEntity();
        entity.id = delivery.getId().value();
        entity.orderId = delivery.getOrderId().value();
        entity.deliveryPersonId = delivery.getDeliveryPersonId() != null ? delivery.getDeliveryPersonId().value() : null;
        entity.pickupAddress = delivery.getPickupAddress();
        entity.deliveryAddress = delivery.getDeliveryAddress();
        entity.status = delivery.getStatus();
        entity.estimatedDeliveryTime = delivery.getEstimatedDeliveryTime();
        entity.assignedAt = delivery.getAssignedAt();
        entity.pickedUpAt = delivery.getPickedUpAt();
        entity.deliveredAt = delivery.getDeliveredAt();
        entity.cancellationReason = delivery.getCancellationReason() != null ? delivery.getCancellationReason().value() : null;
        return entity;
    }

    public Delivery toDomain() {
        return Delivery.reconstruct(
                DeliveryId.of(this.id),
                OrderId.of(this.orderId),
                this.deliveryPersonId != null ? DeliveryPersonId.of(this.deliveryPersonId) : null,
                this.pickupAddress,
                this.deliveryAddress,
                this.status,
                this.estimatedDeliveryTime,
                this.assignedAt,
                this.pickedUpAt,
                this.deliveredAt,
                this.cancellationReason != null ? new CancellationReason(this.cancellationReason) : null
        );
    }
}