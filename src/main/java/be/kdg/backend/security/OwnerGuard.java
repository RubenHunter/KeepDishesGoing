package be.kdg.backend.security;

import be.kdg.backend.domain.restaurant.IRestaurantRepository;
import be.kdg.backend.domain.restaurant.RestaurantId;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component("ownerGuard")
@RequiredArgsConstructor
public class OwnerGuard {
    private final IRestaurantRepository restaurantRepository;

    public boolean canManageRestaurant(UUID restaurantId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwt) || !auth.isAuthenticated()) {
            return false; // unauthenticated -> 401/403
        }

        String sub = jwt.getToken().getSubject();
        if (sub == null || sub.isBlank()) {
            return false;
        }

        Optional<UUID> ownerOpt = restaurantRepository.getOwnerId(new RestaurantId(restaurantId));
        // Deny by default — a restaurant with no assigned owner cannot be managed by anyone (exam-grade authz).
        if (ownerOpt.isEmpty()) {
            return false;
        }

        try {
            UUID requester = UUID.fromString(sub);
            return ownerOpt.get().equals(requester);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
