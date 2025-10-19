package be.kdg.backend.infrastructure;
import be.kdg.backend.domain.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_persons", schema = "delivery")
@Getter
@NoArgsConstructor
public class DeliveryPersonJpaEntity {
    @Id
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "assigned_delivery_id")
    private String assignedDeliveryId;

    @Column(name = "assignment_time")
    private LocalDateTime assignmentTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static DeliveryPersonJpaEntity fromDomain(DeliveryPerson deliveryPerson) {
        DeliveryPersonJpaEntity entity = new DeliveryPersonJpaEntity();
        entity.id = deliveryPerson.getId().value();
        entity.name = deliveryPerson.getName().value();
        entity.vehicleType = deliveryPerson.getVehicleType();
        entity.isAvailable = deliveryPerson.isAvailable();
        entity.latitude = deliveryPerson.getCurrentLocation().latitude();
        entity.longitude = deliveryPerson.getCurrentLocation().longitude();
        entity.assignedDeliveryId = deliveryPerson.getAssignedDeliveryId() != null ? deliveryPerson.getAssignedDeliveryId().value() : null;
        entity.assignmentTime = deliveryPerson.getAssignmentTime();
        return entity;
    }

    public DeliveryPerson toDomain() {
        DeliveryPerson deliveryPerson = new DeliveryPerson(
                DeliveryPersonId.of(this.id),
                new PersonName(this.name),
                this.vehicleType,
                this.isAvailable,
                new Location(this.latitude, this.longitude)
        );

        if (this.assignedDeliveryId != null) {
            deliveryPerson.assignDelivery(DeliveryId.of(this.assignedDeliveryId));
        }

        return deliveryPerson;
    }
}