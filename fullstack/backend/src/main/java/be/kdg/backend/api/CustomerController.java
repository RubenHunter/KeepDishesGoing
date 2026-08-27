package be.kdg.backend.api;

import be.kdg.backend.application.CustomerService;
import be.kdg.backend.domain.customer.Customer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Customer profile endpoints — account settings (name, contact email, home
 * address) keyed by the Keycloak subject so they follow the user across devices.
 * The subject is the customerId; it is derived from the JWT, never from the body/path.
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PutMapping
    public ResponseEntity<Void> saveProfile(JwtAuthenticationToken auth, @Valid @RequestBody ProfileDto dto) {
        customerService.saveProfile(
                customerId(auth),
                dto.name(), dto.email(), dto.street(), dto.number(),
                dto.postalCode(), dto.city(), dto.country());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<ProfileDto> getProfile(JwtAuthenticationToken auth) {
        return customerService.getProfile(customerId(auth))
                .map(c -> ResponseEntity.ok(ProfileDto.from(c)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static UUID customerId(JwtAuthenticationToken auth) {
        return UUID.fromString(auth.getToken().getSubject());
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
