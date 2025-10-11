package be.kdg.backend.application;

import be.kdg.backend.domain.restaurant.RestaurantId;
import be.kdg.backend.infrastructure.jpa.JpaScheduledPublishEntity;
import be.kdg.backend.infrastructure.jpa.JpaScheduledPublishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Component
public class ScheduledPublishProcessor {
    private final JpaScheduledPublishRepository scheduledRepo;
    private final DishService dishService;

    public ScheduledPublishProcessor(JpaScheduledPublishRepository scheduledRepo, DishService dishService) {
        this.scheduledRepo = scheduledRepo;
        this.dishService = dishService;
    }

    // run every 30s
    @Scheduled(fixedDelayString = "PT30S")
    @Scheduled(fixedRateString = "${publish.scheduler.rate:30000}")
    @jakarta.transaction.Transactional // ensures the query with lock/for update runs in a TX
    public void tick() {

        List<JpaScheduledPublishEntity> due = scheduledRepo.findDueForUpdate(LocalDateTime.now());
        for (JpaScheduledPublishEntity job : due) {
            processJob(job.getId());
        }
    }

    @Transactional
    public void processJob(java.util.UUID jobId) {
        JpaScheduledPublishEntity job = scheduledRepo.findById(jobId).orElse(null);
        if (job == null) return;
        if (job.getStatus() != JpaScheduledPublishEntity.Status.PENDING) return;

        job.markRunning();
        scheduledRepo.save(job);

        try {
            dishService.publishAllDraftDishes(new RestaurantId(job.getRestaurantId()));
            job.markDone();
        } catch (Exception ex) {
            job.markFailed(ex.getMessage());
        }
        scheduledRepo.save(job);
    }
}
