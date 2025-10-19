package be.kdg.backend.api;
import be.kdg.backend.application.DeliveryApplicationService;
import be.kdg.backend.application.DeliveryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delivery-persons")
@RequiredArgsConstructor
public class DeliveryPersonAssignmentController {
    private final DeliveryApplicationService deliveryService;

    @GetMapping("/{deliveryPersonId}/active-deliveries")
    public ResponseEntity<List<DeliveryResponse>> getActiveDeliveriesForDeliveryPerson(
            @PathVariable String deliveryPersonId) {
        return ResponseEntity.ok(deliveryService.getActiveDeliveriesForDeliveryPerson(deliveryPersonId));
    }

    @GetMapping("/{deliveryPersonId}/has-active-assignment")
    public ResponseEntity<Boolean> hasActiveAssignment(@PathVariable String deliveryPersonId) {
        return ResponseEntity.ok(deliveryService.hasActiveAssignment(deliveryPersonId));
    }
}