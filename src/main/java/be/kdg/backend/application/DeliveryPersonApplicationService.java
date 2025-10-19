package be.kdg.backend.application;

import be.kdg.backend.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DeliveryPersonApplicationService {
    private final DeliveryPersonRepository deliveryPersonRepository;

    public DeliveryPersonId createDeliveryPerson(String name, String vehicleType, double latitude, double longitude) {
        log.info("Creating delivery person: {}", name);

        DeliveryPerson deliveryPerson = new DeliveryPerson(
                DeliveryPersonId.generate(),
                new PersonName(name),
                VehicleType.valueOf(vehicleType.toUpperCase()),
                true,
                new Location(latitude, longitude)
        );

        DeliveryPerson savedPerson = deliveryPersonRepository.save(deliveryPerson);

        log.info("Delivery person created successfully: {}", savedPerson.getId().value());
        return savedPerson.getId();
    }

    public void updateAvailability(String deliveryPersonId, boolean available) {
        log.info("Updating availability for delivery person {} to {}", deliveryPersonId, available);

        DeliveryPerson deliveryPerson = deliveryPersonRepository.findById(DeliveryPersonId.of(deliveryPersonId))
                .orElseThrow(() -> new DeliveryPersonNotFoundException(deliveryPersonId));

        deliveryPerson.updateAvailability(available);
        deliveryPersonRepository.save(deliveryPerson);

        log.info("Availability updated for delivery person {}: {}", deliveryPersonId, available);
    }

    public void updateLocation(String deliveryPersonId, double latitude, double longitude) {
        log.info("Updating location for delivery person {} to ({}, {})", deliveryPersonId, latitude, longitude);

        DeliveryPerson deliveryPerson = deliveryPersonRepository.findById(DeliveryPersonId.of(deliveryPersonId))
                .orElseThrow(() -> new DeliveryPersonNotFoundException(deliveryPersonId));

        deliveryPerson.updateLocation(new Location(latitude, longitude));
        deliveryPersonRepository.save(deliveryPerson);

        log.info("Location updated for delivery person {}", deliveryPersonId);
    }

    public DeliveryPersonResponse getDeliveryPerson(String deliveryPersonId) {
        DeliveryPerson deliveryPerson = deliveryPersonRepository.findById(DeliveryPersonId.of(deliveryPersonId))
                .orElseThrow(() -> new DeliveryPersonNotFoundException(deliveryPersonId));

        return DeliveryPersonResponse.fromDomain(deliveryPerson);
    }

    public List<DeliveryPersonResponse> getAvailableDeliveryPersons() {
        return deliveryPersonRepository.findByIsAvailable(true).stream()
                .map(DeliveryPersonResponse::fromDomain)
                .toList();
    }

    public List<DeliveryPersonResponse> getAllDeliveryPersons() {
        return deliveryPersonRepository.findAll().stream()
                .map(DeliveryPersonResponse::fromDomain)
                .toList();
    }
}