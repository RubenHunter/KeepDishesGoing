package be.kdg.backend.infrastructure.security;

import be.kdg.backend.domain.driver.DeliveryPersonRepository;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Guard bean — restrict access by the JWT subject = driver id, like OwnerGuard in restaurant-service.
 * Used by {@code @PreAuthorize("@driverGuard.canAccessPayouts(#driverId)")}.
 */
@Component("driverGuard")
@RequiredArgsConstructor
public class DriverGuard {

    private final DeliveryPersonRepository driverRepository;

    public boolean canAccessPayouts(UUID driverId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwt) || !auth.isAuthenticated()) return false;
        String sub = jwt.getToken().getSubject();
        if (sub == null || sub.isBlank()) return false;

        // Admin role bypasses
        var roles = auth.getAuthorities();
        for (var r : roles) {
            if ("admin".equalsIgnoreCase(r.getAuthority())) return true;
        }

        try {
            UUID requester = UUID.fromString(sub);
            if (!requester.equals(driverId)) return false;
            return driverRepository.findById(DeliveryPersonId.of(driverId)).isPresent();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}