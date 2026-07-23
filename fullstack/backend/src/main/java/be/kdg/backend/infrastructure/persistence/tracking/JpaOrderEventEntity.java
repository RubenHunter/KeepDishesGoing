package be.kdg.backend.infrastructure.persistence.tracking;

import be.kdg.backend.application.tracking.OrderEventEntry;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_event_history", schema = "ordering")
public class JpaOrderEventEntity {

    @Id
    @GeneratedValue
    private Long id;

    private UUID orderId;
    private String eventType;
    private LocalDateTime occurredAt;
    @Column(length = 4000)
    private String payloadJson;

    public JpaOrderEventEntity() {}

    public static JpaOrderEventEntity from(OrderEventEntry e) {
        JpaOrderEventEntity entity = new JpaOrderEventEntity();
        entity.id = e.id();
        entity.orderId = e.orderId();
        entity.eventType = e.eventType();
        entity.occurredAt = e.occurredAt();
        entity.payloadJson = e.payloadJson();
        return entity;
    }

    public OrderEventEntry toDomain() {
        return new OrderEventEntry(id, orderId, eventType, occurredAt, payloadJson);
    }

    public UUID getOrderId() { return orderId; }
    public String getEventType() { return eventType; }
}