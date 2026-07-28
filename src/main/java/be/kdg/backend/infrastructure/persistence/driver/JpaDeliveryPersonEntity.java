package be.kdg.backend.infrastructure.persistence.driver;

import be.kdg.backend.domain.driver.DeliveryPerson;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_persons", schema = "delivery")
public class JpaDeliveryPersonEntity {

    @Id
    private UUID id;
    private String name;
    private String vehicleType;
    private boolean available;
    private UUID assignedDeliveryId;
    private LocalDateTime assignmentTime;
    private LocalDateTime updatedAt;

    public JpaDeliveryPersonEntity() {}

    public static JpaDeliveryPersonEntity from(DeliveryPerson p) {
        JpaDeliveryPersonEntity e = new JpaDeliveryPersonEntity();
        e.id = p.id().value();
        e.name = p.name();
        e.vehicleType = p.vehicleType();
        e.available = p.available();
        e.assignedDeliveryId = p.assignedDeliveryId() == null ? null : p.assignedDeliveryId().value();
        e.assignmentTime = p.assignmentTime();
        e.updatedAt = p.updatedAt();
        return e;
    }

    public DeliveryPerson toDomain() {
        return DeliveryPerson.rehydrate(
                DeliveryPersonId.of(id),
                name,
                vehicleType,
                available,
                assignedDeliveryId == null ? null : be.kdg.backend.domain.shared.DeliveryId.of(assignedDeliveryId),
                assignmentTime,
                updatedAt
        );
    }
}