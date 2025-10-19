package be.kdg.backend.api;

import be.kdg.backend.application.AssignDeliveryPersonCommand;
import be.kdg.backend.application.DeliveryApplicationService;
import be.kdg.backend.application.ReassignDeliveryPersonCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryAssignmentController {
    private final DeliveryApplicationService deliveryService;

    @PostMapping("/{deliveryId}/assign")
    public ResponseEntity<Void> assignDeliveryPerson(
            @PathVariable String deliveryId,
            @Valid @RequestBody AssignDeliveryPersonCommand command) {
        deliveryService.assignDeliveryPerson(command);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{deliveryId}/reassign")
    public ResponseEntity<Void> reassignDeliveryPerson(
            @PathVariable String deliveryId,
            @Valid @RequestBody ReassignDeliveryPersonCommand command) {
        deliveryService.reassignDeliveryPerson(command);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{deliveryId}/has-assigned-person")
    public ResponseEntity<Boolean> hasAssignedDeliveryPerson(@PathVariable String deliveryId) {
        return ResponseEntity.ok(deliveryService.hasAssignedDeliveryPerson(deliveryId));
    }

    @GetMapping("/{deliveryId}/assigned-person")
    public ResponseEntity<AssignedPersonResponse> getAssignedDeliveryPerson(@PathVariable String deliveryId) {
        String assignedPersonId = deliveryService.getAssignedDeliveryPersonId(deliveryId);

        if (assignedPersonId == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(new AssignedPersonResponse(assignedPersonId));
    }

    public record AssignedPersonResponse(String deliveryPersonId) {}
}