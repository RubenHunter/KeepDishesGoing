package be.kdg.backend.application;

import be.kdg.backend.domain.DomainConflictException;
import be.kdg.backend.domain.NotFoundException;
import be.kdg.backend.domain.restaurant.IRestaurantRepository;
import be.kdg.backend.domain.restaurant.Restaurant;
import be.kdg.backend.domain.restaurant.RestaurantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Enforces US11 (orders only within opening hours) and US14 (accepted orders feasible within
 * opening hours) at the moment the owner accepts an order. Orchestration only — the open/close
 * and feasibility invariants live on the {@link Restaurant} aggregate.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAcceptanceService {

    private final IRestaurantRepository restaurantRepository;
    private final PrepTimeEstimator prepTimeEstimator;

    public void verifyCanAccept(UUID restaurantId, LocalDateTime at) {
        Restaurant restaurant = restaurantRepository.getById(new RestaurantId(restaurantId))
                .orElseThrow(() -> new NotFoundException("Restaurant not found"));

        if (!restaurant.isOpenOn(at)) {
            Optional<LocalDateTime> next = restaurant.nextOpeningAfter(at);
            String when = next.map(n -> ", opens " + n).orElse("");
            throw new DomainConflictException("Restaurant closed" + when);
        }

        if (!restaurant.isOpenThrough(at, at.plus(prepTimeEstimator.estimate()))) {
            throw new DomainConflictException("Order cannot be prepared before closing time");
        }
    }
}
