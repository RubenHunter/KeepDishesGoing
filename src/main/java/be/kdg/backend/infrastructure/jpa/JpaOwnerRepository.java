package be.kdg.backend.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaOwnerRepository extends JpaRepository<JpaOwnerEntity, UUID> {
    Optional<JpaOwnerEntity> findByExternalSubjectId(String externalSubjectId);
}
