package be.kdg.backend.infrastructure.persistence.cart;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataCartJpaRepository extends JpaRepository<JpaCartEntity, UUID> {
    Optional<JpaCartEntity> findById(UUID id);
}