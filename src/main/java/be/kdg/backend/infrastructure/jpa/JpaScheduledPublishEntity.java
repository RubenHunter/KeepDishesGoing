package be.kdg.backend.infrastructure.jpa;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "scheduled_publish")
@Getter
public class JpaScheduledPublishEntity {
    public enum Status { PENDING, RUNNING, DONE, FAILED }

    @Id
    private UUID id;

    @Column(name = "restaurant_id", nullable = false)
    private UUID restaurantId;

    @Column(name = "publish_at", nullable = false)
    private LocalDateTime publishAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected JpaScheduledPublishEntity() {}

    public JpaScheduledPublishEntity(UUID id, UUID restaurantId, LocalDateTime publishAt) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.publishAt = publishAt;
        this.status = Status.PENDING;
        this.attempts = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

//    public UUID getId() { return id; }
//    public UUID getRestaurantId() { return restaurantId; }
//    public LocalDateTime getPublishAt() { return publishAt; }
//    public Status getStatus() { return status; }
//    public int getAttempts() { return attempts; }
//    public String getLastError() { return lastError; }

    public void markRunning() {
        this.status = Status.RUNNING;
        this.attempts += 1;
        this.lastError = null;
        this.onUpdate();
    }
    public void markDone() {
        this.status = Status.DONE;
        this.onUpdate();
    }
    public void markFailed(String error) {
        this.status = Status.FAILED;
        this.lastError = error != null ? error.substring(0, Math.min(error.length(), 500)) : null;
        this.onUpdate();
    }
}
