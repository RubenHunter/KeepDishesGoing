package be.kdg.backend.api;

import be.kdg.backend.application.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {
    private final DeliveryApplicationService deliveryService;

    @PostMapping
    public ResponseEntity<DeliveryResponse> createDelivery(@Valid @RequestBody CreateDeliveryCommand command) {
        var deliveryId = deliveryService.createDelivery(command);

        return ResponseEntity.created(URI.create("/api/deliveries/" + deliveryId.value()))
                .body(deliveryService.getDelivery(deliveryId.value()));
    }

    @PostMapping("/{deliveryId}/assign")
    public ResponseEntity<Void> assignDeliveryPerson(
            @PathVariable String deliveryId,
            @Valid @RequestBody AssignDeliveryPersonCommand command) {
        deliveryService.assignDeliveryPerson(command);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{deliveryId}/pickup")
    public ResponseEntity<Void> markPickedUp(@PathVariable String deliveryId) {
        deliveryService.markPickedUp(deliveryId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{deliveryId}/transit")
    public ResponseEntity<Void> markInTransit(@PathVariable String deliveryId) {
        deliveryService.markInTransit(deliveryId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{deliveryId}/deliver")
    public ResponseEntity<Void> markDelivered(@PathVariable String deliveryId) {
        deliveryService.markDelivered(deliveryId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{deliveryId}/cancel")
    public ResponseEntity<Void> cancelDelivery(
            @PathVariable String deliveryId,
            @Valid @RequestBody CancelDeliveryCommand command) {
        deliveryService.cancelDelivery(command);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{deliveryId}")
    public ResponseEntity<DeliveryResponse> getDelivery(@PathVariable String deliveryId) {
        return ResponseEntity.ok(deliveryService.getDelivery(deliveryId));
    }

    @GetMapping
    public ResponseEntity<List<DeliveryResponse>> getDeliveries(@RequestParam(required = false) String status) {
        if (status != null) {
            return ResponseEntity.ok(deliveryService.getDeliveriesByStatus(status));
        }
        return ResponseEntity.ok(deliveryService.getAllDeliveries());
    }
}