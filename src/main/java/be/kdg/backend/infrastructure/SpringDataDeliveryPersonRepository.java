package be.kdg.backend.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataDeliveryPersonRepository extends JpaRepository<DeliveryPersonJpaEntity, String> {

    List<DeliveryPersonJpaEntity> findByIsAvailable(boolean isAvailable);

    @Query("SELECT dp FROM DeliveryPersonJpaEntity dp WHERE dp.isAvailable = true " +
            "AND FUNCTION('calculate_distance', dp.latitude, dp.longitude, :lat, :lon) <= :radius")
    List<DeliveryPersonJpaEntity> findAvailableWithinRadius(@Param("lat") double latitude,
                                                            @Param("lon") double longitude,
                                                            @Param("radius") double radiusKm);
}