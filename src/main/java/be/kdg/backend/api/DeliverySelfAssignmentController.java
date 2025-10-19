package be.kdg.backend.api;

import be.kdg.backend.application.DeliveryApplicationService;
import be.kdg.backend.application.DeliveryResponse;
import be.kdg.backend.application.ListAvailableDeliveriesCommand;
import be.kdg.backend.application.SelfAssignDeliveryCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliverySelfAssignmentController {
    private final DeliveryApplicationService deliveryService;

    @PostMapping("/self-assign")
    public ResponseEntity<Void> selfAssignDelivery(@Valid @RequestBody SelfAssignDeliveryCommand command) {
        deliveryService.selfAssignDelivery(command);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/available")
    public ResponseEntity<List<DeliveryResponse>> getAvailableDeliveries(
            @RequestParam String deliveryPersonId,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double maxRadiusKm) {

        ListAvailableDeliveriesCommand command = new ListAvailableDeliveriesCommand(
                deliveryPersonId, latitude, longitude, maxRadiusKm
        );

        return ResponseEntity.ok(deliveryService.getAvailableDeliveriesForSelfAssignment(command));
    }

    @GetMapping("/available/all")
    public ResponseEntity<List<DeliveryResponse>> getAllAvailableDeliveries() {
        return ResponseEntity.ok(deliveryService.getAllAvailableDeliveries());
    }

    @PostMapping("/{deliveryId}/make-unavailable")
    public ResponseEntity<Void> markDeliveryAsUnavailable(@PathVariable String deliveryId) {
        deliveryService.markDeliveryAsUnavailable(deliveryId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{deliveryId}/make-available")
    public ResponseEntity<Void> markDeliveryAsAvailable(@PathVariable String deliveryId) {
        deliveryService.markDeliveryAsAvailable(deliveryId);
        return ResponseEntity.ok().build();
    }
}