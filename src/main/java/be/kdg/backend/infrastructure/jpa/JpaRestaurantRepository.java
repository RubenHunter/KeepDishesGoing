package be.kdg.backend.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JpaRestaurantRepository extends JpaRepository<JpaRestaurantEntity, UUID> {

    Optional<JpaRestaurantEntity> findByDishes_Id(UUID dishId);

    // New: fetch only ownerId to avoid touching lazy collections
    @Query("select r.ownerId from JpaRestaurantEntity r where r.id = :id")
    Optional<UUID> findOwnerIdById(@Param("id") UUID id);

    // US1
    boolean existsByOwnerId(UUID ownerId);

    Optional<JpaRestaurantEntity> findByOwnerId(UUID ownerId);
}
