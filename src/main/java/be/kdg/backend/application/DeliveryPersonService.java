package be.kdg.backend.application;

import be.kdg.backend.domain.driver.DeliveryPerson;
import be.kdg.backend.domain.driver.DeliveryPersonAlreadyAssignedException;
import be.kdg.backend.domain.driver.DeliveryPersonRepository;
import be.kdg.backend.domain.shared.DeliveryId;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * DeliveryPerson application service. Orchestrates driver aggregates — no domain logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryPersonService {

    private final DeliveryPersonRepository driverRepository;

    @Transactional
    public DeliveryPersonId registerDriver(String name, String vehicle) {
        DeliveryPerson dp = new DeliveryPerson(DeliveryPersonId.generate(), name, vehicle, true);
        driverRepository.save(dp);
        log.info("Registered driver {} ({})", dp.id(), name);
        return dp.id();
    }

    @Transactional(readOnly = true)
    public DeliveryPerson get(DeliveryPersonId id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new be.kdg.backend.domain.driver.DeliveryPersonNotFoundException(id));
    }

    /** Called by Spring event listener after Delivery AR is updated — separate tx (one-agg-per-tx). */
    @Transactional
    public void assignDriver(DeliveryId deliveryId, DeliveryPersonId driverId, LocalDateTime at) {
        DeliveryPerson dp = get(driverId);
        try {
            dp.assignDelivery(deliveryId, at);
        } catch (DeliveryPersonAlreadyAssignedException ex) {
            log.warn("Driver {} already has an assignment (US31) — courier should not have been able to claim", driverId);
            throw ex;
        }
        driverRepository.save(dp);
    }

    @Transactional
    public void releaseDriver(DeliveryPersonId driverId, LocalDateTime at) {
        DeliveryPerson dp = get(driverId);
        dp.release(at);
        driverRepository.save(dp);
    }

    @Transactional
    public void setAvailability(DeliveryPersonId driverId, boolean available, LocalDateTime at) {
        DeliveryPerson dp = get(driverId);
        dp.updateAvailability(available, at);
        driverRepository.save(dp);
    }
}