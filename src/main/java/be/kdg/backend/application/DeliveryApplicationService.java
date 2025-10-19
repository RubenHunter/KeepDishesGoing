package be.kdg.backend.application;

import be.kdg.backend.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DeliveryApplicationService {
    private final DeliveryRepository deliveryRepository;
    private final DeliveryPersonRepository deliveryPersonRepository;
    private final DeliveryAssignmentService deliveryAssignmentService;

    public DeliveryId createDelivery(CreateDeliveryCommand command) {
        log.info("Creating delivery for order: {}", command.orderId());

        Delivery delivery = new Delivery(
                DeliveryId.generate(),
                OrderId.of(command.orderId()),
                command.pickupAddress(),
                command.deliveryAddress()
        );

        Delivery savedDelivery = deliveryRepository.save(delivery);

        log.info("Delivery created successfully: {}", savedDelivery.getId().value());
        return savedDelivery.getId();
    }

    public void assignDeliveryPerson(AssignDeliveryPersonCommand command) {
        log.info("Assigning delivery person {} to delivery {}", command.deliveryPersonId(), command.deliveryId());

        Delivery delivery = deliveryRepository.findById(DeliveryId.of(command.deliveryId()))
                .orElseThrow(() -> new DeliveryNotFoundException(command.deliveryId()));

        DeliveryPerson deliveryPerson = deliveryPersonRepository.findById(DeliveryPersonId.of(command.deliveryPersonId()))
                .orElseThrow(() -> new DeliveryPersonNotFoundException(command.deliveryPersonId()));

        double maxRadiusKm = 10.0; // Configurable
        if (!deliveryAssignmentService.canAssignDelivery(deliveryPerson, delivery, maxRadiusKm)) {
            throw new IllegalStateException("Delivery person cannot be assigned to this delivery");
        }

        delivery.assignDeliveryPerson(deliveryPerson.getId(), LocalDateTime.now());
        deliveryPerson.assignDelivery(delivery.getId());

        deliveryRepository.save(delivery);
        deliveryPersonRepository.save(deliveryPerson);

        log.info("Delivery person {} assigned to delivery {}", command.deliveryPersonId(), command.deliveryId());
    }

    public void markPickedUp(String deliveryId) {
        log.info("Marking delivery as picked up: {}", deliveryId);

        Delivery delivery = deliveryRepository.findById(DeliveryId.of(deliveryId))
                .orElseThrow(() -> new DeliveryNotFoundException(deliveryId));

        delivery.markPickedUp();
        deliveryRepository.save(delivery);

        log.info("Delivery marked as picked up: {}", deliveryId);
    }

    public void markInTransit(String deliveryId) {
        log.info("Marking delivery as in transit: {}", deliveryId);

        Delivery delivery = deliveryRepository.findById(DeliveryId.of(deliveryId))
                .orElseThrow(() -> new DeliveryNotFoundException(deliveryId));

        delivery.markInTransit();
        deliveryRepository.save(delivery);

        log.info("Delivery marked as in transit: {}", deliveryId);
    }

    public void markDelivered(String deliveryId) {
        log.info("Marking delivery as delivered: {}", deliveryId);

        Delivery delivery = deliveryRepository.findById(DeliveryId.of(deliveryId))
                .orElseThrow(() -> new DeliveryNotFoundException(deliveryId));

        delivery.markDelivered();
        deliveryRepository.save(delivery);

        log.info("Delivery marked as delivered: {}", deliveryId);
    }

    public void cancelDelivery(CancelDeliveryCommand command) {
        log.info("Cancelling delivery {} with reason: {}", command.deliveryId(), command.reason());

        Delivery delivery = deliveryRepository.findById(DeliveryId.of(command.deliveryId()))
                .orElseThrow(() -> new DeliveryNotFoundException(command.deliveryId()));

        if (delivery.getDeliveryPersonId() != null) {
            DeliveryPerson deliveryPerson = deliveryPersonRepository.findById(delivery.getDeliveryPersonId())
                    .orElseThrow(() -> new DeliveryPersonNotFoundException(delivery.getDeliveryPersonId().value()));
            deliveryPerson.unassignDelivery();
            deliveryPersonRepository.save(deliveryPerson);
        }

        delivery.cancelDelivery(new CancellationReason(command.reason()));
        deliveryRepository.save(delivery);

        log.info("Delivery cancelled: {}", command.deliveryId());
    }

    public DeliveryResponse getDelivery(String deliveryId) {
        Delivery delivery = deliveryRepository.findById(DeliveryId.of(deliveryId))
                .orElseThrow(() -> new DeliveryNotFoundException(deliveryId));

        return DeliveryResponse.fromDomain(delivery);
    }

    public List<DeliveryResponse> getDeliveriesByStatus(String status) {
        DeliveryStatus deliveryStatus = DeliveryStatus.valueOf(status.toUpperCase());
        return deliveryRepository.findByStatus(deliveryStatus).stream()
                .map(DeliveryResponse::fromDomain)
                .toList();
    }

    public List<DeliveryResponse> getAllDeliveries() {
        return deliveryRepository.findAll().stream()
                .map(DeliveryResponse::fromDomain)
                .toList();
    }
}