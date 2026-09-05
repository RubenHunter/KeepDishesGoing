package be.kdg.backend.domain.scheduling;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IScheduledPublishRepository {
    List<ScheduledPublishJob> findDueForUpdate(LocalDateTime now);
    Optional<ScheduledPublishJob> getById(UUID id);
    void save(ScheduledPublishJob job);
}
