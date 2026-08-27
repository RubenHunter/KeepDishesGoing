package be.kdg.backend.infrastructure;

import be.kdg.backend.domain.scheduling.IScheduledPublishRepository;
import be.kdg.backend.domain.scheduling.ScheduledPublishJob;
import be.kdg.backend.infrastructure.jpa.JpaScheduledPublishEntity;
import be.kdg.backend.infrastructure.jpa.JpaScheduledPublishRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Primary
@Repository
@Transactional(readOnly = true)
public class DbScheduledPublishRepository implements IScheduledPublishRepository {
    private final JpaScheduledPublishRepository jpa;

    public DbScheduledPublishRepository(JpaScheduledPublishRepository jpa) {
        this.jpa = jpa;
    }

    /**
     * The JPA query carries a pessimistic row lock (`FOR NO KEY UPDATE`) so two
     * instances cannot claim the same job — that lock is rejected inside a
     * read-only transaction, hence this override to a read-write one.
     */
    @Override
    @Transactional
    public List<ScheduledPublishJob> findDueForUpdate(LocalDateTime now) {
        return jpa.findDueForUpdate(now).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<ScheduledPublishJob> getById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional
    public void save(ScheduledPublishJob job) {
        // If exists, transition using entity methods; else create a new one.
        JpaScheduledPublishEntity entity = jpa.findById(job.getId()).orElse(null);
        if (entity == null) {
            entity = new JpaScheduledPublishEntity(job.getId(), job.getRestaurantId(), job.getPublishAt());
            // Initial state is PENDING by constructor; persist it.
            jpa.save(entity);
            entity = jpa.findById(job.getId()).orElseThrow();
        }
        // Reflect desired status
        switch (job.getStatus()) {
            case RUNNING -> {
                if (entity.getStatus() == JpaScheduledPublishEntity.Status.PENDING) {
                    entity.markRunning();
                }
            }
            case DONE -> {
                if (entity.getStatus() != JpaScheduledPublishEntity.Status.DONE) {
                    entity.markDone();
                }
            }
            case FAILED -> {
                if (entity.getStatus() != JpaScheduledPublishEntity.Status.FAILED) {
                    entity.markFailed(job.getLastError());
                }
            }
            case PENDING -> {
                // nothing; initial persisted state is PENDING
            }
        }
        jpa.save(entity);
    }

    private ScheduledPublishJob toDomain(JpaScheduledPublishEntity e) {
        return ScheduledPublishJob.rehydrate(
                e.getId(),
                e.getRestaurantId(),
                e.getPublishAt(),
                switch (e.getStatus()) {
                    case PENDING -> ScheduledPublishJob.Status.PENDING;
                    case RUNNING -> ScheduledPublishJob.Status.RUNNING;
                    case DONE -> ScheduledPublishJob.Status.DONE;
                    case FAILED -> ScheduledPublishJob.Status.FAILED;
                },
                e.getAttempts(),
                e.getLastError(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
