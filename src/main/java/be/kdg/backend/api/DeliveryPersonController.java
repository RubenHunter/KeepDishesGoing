package be.kdg.backend.api;
import be.kdg.backend.application.DeliveryPersonApplicationService;
import be.kdg.backend.application.DeliveryPersonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/delivery-persons")
@RequiredArgsConstructor
public class DeliveryPersonController {
    private final DeliveryPersonApplicationService deliveryPersonService;

    @PostMapping
    public ResponseEntity<DeliveryPersonResponse> createDeliveryPerson(
            @RequestParam String name,
            @RequestParam String vehicleType,
            @RequestParam double latitude,
            @RequestParam double longitude) {

        var personId = deliveryPersonService.createDeliveryPerson(name, vehicleType, latitude, longitude);

        return ResponseEntity.created(URI.create("/api/delivery-persons/" + personId.value()))
                .body(deliveryPersonService.getDeliveryPerson(personId.value()));
    }

    @PutMapping("/{deliveryPersonId}/availability")
    public ResponseEntity<Void> updateAvailability(
            @PathVariable String deliveryPersonId,
            @RequestParam boolean available) {

        deliveryPersonService.updateAvailability(deliveryPersonId, available);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{deliveryPersonId}/location")
    public ResponseEntity<Void> updateLocation(
            @PathVariable String deliveryPersonId,
            @RequestParam double latitude,
            @RequestParam double longitude) {

        deliveryPersonService.updateLocation(deliveryPersonId, latitude, longitude);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{deliveryPersonId}")
    public ResponseEntity<DeliveryPersonResponse> getDeliveryPerson(@PathVariable String deliveryPersonId) {
        return ResponseEntity.ok(deliveryPersonService.getDeliveryPerson(deliveryPersonId));
    }

    @GetMapping("/available")
    public ResponseEntity<List<DeliveryPersonResponse>> getAvailableDeliveryPersons() {
        return ResponseEntity.ok(deliveryPersonService.getAvailableDeliveryPersons());
    }

    @GetMapping
    public ResponseEntity<List<DeliveryPersonResponse>> getAllDeliveryPersons() {
        return ResponseEntity.ok(deliveryPersonService.getAllDeliveryPersons());
    }
}