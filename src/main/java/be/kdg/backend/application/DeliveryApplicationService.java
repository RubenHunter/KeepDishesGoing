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

    public void assignDeliveryPerson(AssignDeliveryPersonCommand command) {
        log.info("Assigning delivery person {} to delivery {}", command.deliveryPersonId(), command.deliveryId());

        Delivery delivery = deliveryRepository.findById(DeliveryId.of(command.deliveryId()))
                .orElseThrow(() -> new DeliveryNotFoundException(command.deliveryId()));

        DeliveryPerson deliveryPerson = deliveryPersonRepository.findById(DeliveryPersonId.of(command.deliveryPersonId()))
                .orElseThrow(() -> new DeliveryPersonNotFoundException(command.deliveryPersonId()));

        deliveryAssignmentService.validateSingleActiveAssignment(deliveryPerson);
        deliveryAssignmentService.validateSingleDeliveryPersonPerDelivery(delivery);

        double maxRadiusKm = 10.0;
        if (!deliveryAssignmentService.canAssignDelivery(deliveryPerson, delivery, maxRadiusKm)) {
            throw new IllegalStateException("Delivery person cannot be assigned to this delivery");
        }

        delivery.assignDeliveryPerson(deliveryPerson.getId(), LocalDateTime.now());
        deliveryPerson.assignDelivery(delivery.getId());

        deliveryRepository.save(delivery);
        deliveryPersonRepository.save(deliveryPerson);

        log.info("Delivery person {} assigned to delivery {}", command.deliveryPersonId(), command.deliveryId());
    }

    public void reassignDeliveryPerson(ReassignDeliveryPersonCommand command) {
        log.info("Reassigning delivery {} to delivery person {}", command.deliveryId(), command.deliveryPersonId());

        Delivery delivery = deliveryRepository.findById(DeliveryId.of(command.deliveryId()))
                .orElseThrow(() -> new DeliveryNotFoundException(command.deliveryId()));

        DeliveryPerson newDeliveryPerson = deliveryPersonRepository.findById(DeliveryPersonId.of(command.deliveryPersonId()))
                .orElseThrow(() -> new DeliveryPersonNotFoundException(command.deliveryPersonId()));

        if (delivery.getDeliveryPersonId() != null) {
            DeliveryPerson currentDeliveryPerson = deliveryPersonRepository.findById(delivery.getDeliveryPersonId())
                    .orElseThrow(() -> new DeliveryPersonNotFoundException(delivery.getDeliveryPersonId().value()));
            currentDeliveryPerson.unassignDelivery();
            deliveryPersonRepository.save(currentDeliveryPerson);
        }

        deliveryAssignmentService.validateSingleActiveAssignment(newDeliveryPerson);

        double maxRadiusKm = 10.0;
        if (!deliveryAssignmentService.canAssignDelivery(newDeliveryPerson, delivery, maxRadiusKm)) {
            throw new IllegalStateException("Delivery person cannot be assigned to this delivery");
        }

        delivery.assignDeliveryPerson(newDeliveryPerson.getId(), LocalDateTime.now());
        newDeliveryPerson.assignDelivery(delivery.getId());

        deliveryRepository.save(delivery);
        deliveryPersonRepository.save(newDeliveryPerson);

        log.info("Delivery {} reassigned to delivery person {}", command.deliveryId(), command.deliveryPersonId());
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

    public boolean hasAssignedDeliveryPerson(String deliveryId) {
        Delivery delivery = deliveryRepository.findById(DeliveryId.of(deliveryId))
                .orElseThrow(() -> new DeliveryNotFoundException(deliveryId));

        return delivery.hasAssignedDeliveryPerson();
    }

    public String getAssignedDeliveryPersonId(String deliveryId) {
        Delivery delivery = deliveryRepository.findById(DeliveryId.of(deliveryId))
                .orElseThrow(() -> new DeliveryNotFoundException(deliveryId));

        return delivery.getDeliveryPersonId() != null ? delivery.getDeliveryPersonId().value() : null;
    }

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

    public boolean hasActiveAssignment(String deliveryPersonId) {
        DeliveryPerson deliveryPerson = deliveryPersonRepository.findById(DeliveryPersonId.of(deliveryPersonId))
                .orElseThrow(() -> new DeliveryPersonNotFoundException(deliveryPersonId));

        return deliveryPerson.hasActiveAssignment();
    }
    public List<DeliveryResponse> getActiveDeliveriesForDeliveryPerson(String deliveryPersonId) {
        DeliveryPersonId personId = DeliveryPersonId.of(deliveryPersonId);
        List<Delivery> deliveries = deliveryRepository.findByDeliveryPersonId(personId);

        return deliveries.stream()
                .filter(delivery -> delivery.getStatus() == DeliveryStatus.ASSIGNED ||
                        delivery.getStatus() == DeliveryStatus.PICKED_UP ||
                        delivery.getStatus() == DeliveryStatus.IN_TRANSIT)
                .map(DeliveryResponse::fromDomain)
                .toList();
    }

}