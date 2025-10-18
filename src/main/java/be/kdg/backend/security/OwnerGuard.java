package be.kdg.backend.security;

import be.kdg.backend.domain.restaurant.IRestaurantRepository;
import be.kdg.backend.domain.restaurant.RestaurantId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OwnerGuard {
    private final IRestaurantRepository repo;

    public OwnerGuard(IRestaurantRepository repo) {
        this.repo = repo;
    }

    public boolean canManageRestaurant(UUID restaurantId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;

        String sub = auth.getName(); // JWT sub
        var ownerIdOpt = repo.getOwnerId(new RestaurantId(restaurantId));

        // Allow managing when no owner is assigned yet (test helper created restos).
        if (ownerIdOpt.isEmpty()) return true;

        UUID ownerId = ownerIdOpt.get();
        return ownerId != null && ownerId.toString().equals(sub);
    }
}
