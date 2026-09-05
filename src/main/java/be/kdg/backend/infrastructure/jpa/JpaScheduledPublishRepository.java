package be.kdg.backend.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface JpaScheduledPublishRepository extends JpaRepository<JpaScheduledPublishEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           select j from JpaScheduledPublishEntity j
           where j.status = 'PENDING' and j.publishAt <= :now
           order by j.publishAt asc
           """)
    List<JpaScheduledPublishEntity> findDueForUpdate(LocalDateTime now);
}
