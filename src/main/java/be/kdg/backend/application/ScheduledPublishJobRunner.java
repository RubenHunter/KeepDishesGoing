package be.kdg.backend.application;

import be.kdg.backend.domain.restaurant.RestaurantId;
import be.kdg.backend.domain.scheduling.IScheduledPublishRepository;
import be.kdg.backend.domain.scheduling.ScheduledPublishJob;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Runs a single scheduled-publish job in its own transaction (US8). Extracted from the scheduler
 * so {@code @Transactional} applies through the Spring proxy (coding-mistakes #15 — no self-invocation).
 */
@Service
public class ScheduledPublishJobRunner {

    private final IScheduledPublishRepository scheduledRepo;
    private final DishService dishService;

    public ScheduledPublishJobRunner(IScheduledPublishRepository scheduledRepo, DishService dishService) {
        this.scheduledRepo = scheduledRepo;
        this.dishService = dishService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void run(UUID jobId) {
        ScheduledPublishJob job = scheduledRepo.getById(jobId).orElse(null);
        if (job == null || job.getStatus() != ScheduledPublishJob.Status.PENDING) {
            return;
        }

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
