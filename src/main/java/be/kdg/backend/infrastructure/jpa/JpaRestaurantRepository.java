package be.kdg.backend.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaRestaurantRepository extends JpaRepository<JpaRestaurantEntity, UUID> {



}
