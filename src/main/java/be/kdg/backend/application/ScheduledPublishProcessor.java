package be.kdg.backend.application;

import be.kdg.backend.domain.scheduling.IScheduledPublishRepository;
import be.kdg.backend.domain.scheduling.ScheduledPublishJob;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler (US8) — periodically finds due publish jobs and delegates each to
 * {@link ScheduledPublishJobRunner} (a separate bean, so the per-job transaction is applied through
 * the Spring proxy — coding-mistakes #15). Single {@code @Scheduled}; rate is configurable.
 */
@Service
public class ScheduledPublishProcessor {

    private final IScheduledPublishRepository scheduledRepo;
    private final ScheduledPublishJobRunner jobRunner;

    public ScheduledPublishProcessor(IScheduledPublishRepository scheduledRepo, ScheduledPublishJobRunner jobRunner) {
        this.scheduledRepo = scheduledRepo;
        this.jobRunner = jobRunner;
    }

    @Scheduled(fixedDelayString = "${publish.scheduler.rate:30000}")
    public void tick() {
        List<ScheduledPublishJob> due = scheduledRepo.findDueForUpdate(LocalDateTime.now());
        for (ScheduledPublishJob job : due) {
            jobRunner.run(job.getId());
        }
    }
}
