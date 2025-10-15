package be.kdg.backend.application;

import be.kdg.backend.domain.restaurant.RestaurantId;
import be.kdg.backend.domain.scheduling.IScheduledPublishRepository;
import be.kdg.backend.domain.scheduling.ScheduledPublishJob;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ScheduledPublishProcessor {
    private final IScheduledPublishRepository scheduledRepo;
    private final DishService dishService;

    public ScheduledPublishProcessor(IScheduledPublishRepository scheduledRepo, DishService dishService) {
        this.scheduledRepo = scheduledRepo;
        this.dishService = dishService;
    }

    // run every 30s
    @Scheduled(fixedDelayString = "PT30S")
    @Scheduled(fixedRateString = "${publish.scheduler.rate:30000}")
    @jakarta.transaction.Transactional
    public void tick() {
        List<ScheduledPublishJob> due = scheduledRepo.findDueForUpdate(LocalDateTime.now());
        for (ScheduledPublishJob job : due) {
            processJob(job.getId());
        }
    }

    @Transactional
    public void processJob(UUID jobId) {
        ScheduledPublishJob job = scheduledRepo.getById(jobId).orElse(null);
        if (job == null) return;
        if (job.getStatus() != ScheduledPublishJob.Status.PENDING) return;

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
