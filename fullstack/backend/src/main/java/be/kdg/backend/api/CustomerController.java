package be.kdg.backend.api;

import be.kdg.backend.application.CustomerService;
import be.kdg.backend.domain.customer.Customer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Customer profile endpoints — account settings (name, contact email, home
 * address) keyed by the Keycloak subject so they follow the user across devices.
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PutMapping("/{customerId}")
    public ResponseEntity<Void> saveProfile(@PathVariable UUID customerId, @Valid @RequestBody ProfileDto dto) {
        customerService.saveProfile(
                customerId,
                dto.name(), dto.email(), dto.street(), dto.number(),
                dto.postalCode(), dto.city(), dto.country());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<ProfileDto> getProfile(@PathVariable UUID customerId) {
        return customerService.getProfile(customerId)
                .map(c -> ResponseEntity.ok(ProfileDto.from(c)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record ProfileDto(
            String name,
            @Email String email,
            String street,
            String number,
            String postalCode,
            String city,
            String country
    ) {
        public static ProfileDto from(Customer c) {
            return new ProfileDto(
                    c.name(), c.email(), c.street(), c.number(),
                    c.postalCode(), c.city(), c.country());
        }
    }
}
