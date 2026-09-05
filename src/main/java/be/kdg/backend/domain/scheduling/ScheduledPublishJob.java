package be.kdg.backend.domain.scheduling;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class ScheduledPublishJob {
    public enum Status { PENDING, RUNNING, DONE, FAILED }

    private final UUID id;
    private final UUID restaurantId;
    private final LocalDateTime publishAt;

    private Status status;
    private int attempts;
    private String lastError;

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private ScheduledPublishJob(UUID id,
                                UUID restaurantId,
                                LocalDateTime publishAt,
                                Status status,
                                int attempts,
                                String lastError,
                                LocalDateTime createdAt,
                                LocalDateTime updatedAt) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.publishAt = publishAt;
        this.status = status;
        this.attempts = attempts;
        this.lastError = lastError;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static ScheduledPublishJob create(UUID id, UUID restaurantId, LocalDateTime publishAt) {
        return new ScheduledPublishJob(id, restaurantId, publishAt, Status.PENDING, 0, null, LocalDateTime.now(), LocalDateTime.now());
    }

    public static ScheduledPublishJob rehydrate(UUID id,
                                                UUID restaurantId,
                                                LocalDateTime publishAt,
                                                Status status,
                                                int attempts,
                                                String lastError,
                                                LocalDateTime createdAt,
                                                LocalDateTime updatedAt) {
        return new ScheduledPublishJob(id, restaurantId, publishAt, status, attempts, lastError, createdAt, updatedAt);
    }

    public void markRunning() {
        if (status == Status.PENDING) {
            status = Status.RUNNING;
            attempts += 1;
            lastError = null;
            touch();
        }
    }

    public void markDone() {
        status = Status.DONE;
        touch();
    }

    public void markFailed(String error) {
        status = Status.FAILED;
        lastError = error;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
