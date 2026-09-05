package be.kdg.backend.application;

import be.kdg.backend.domain.DomainConflictException;
import be.kdg.backend.domain.restaurant.IRestaurantRepository;
import be.kdg.backend.domain.restaurant.Restaurant;
import be.kdg.backend.domain.restaurant.RestaurantId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

/**
 * US11 (closed → reject accept) + US14 (prep must finish before closing) at the owner-accept moment.
 */
@ExtendWith(MockitoExtension.class)
class OrderAcceptanceServiceTest {

    @Mock
    private IRestaurantRepository restaurantRepository;

    @Mock
    private PrepTimeEstimator prepTimeEstimator;

    @InjectMocks
    private OrderAcceptanceService sut;

    private final UUID restaurantId = UUID.randomUUID();

    private Restaurant activeRestaurant(String openingHours) {
        Restaurant r = Restaurant.create(
                "Resto", "Street 1", "r@x.be", openingHours, "logo.png", UUID.randomUUID());
        r.open();
        return r;
    }

    @Test
    void acceptsWhenOpenAndFeasible() {
        Restaurant r = activeRestaurant("Mon-Sun 11:00-23:00");
        given(restaurantRepository.getById(new RestaurantId(restaurantId))).willReturn(Optional.of(r));
        given(prepTimeEstimator.estimate()).willReturn(Duration.ofMinutes(30));

        assertDoesNotThrow(() -> sut.verifyCanAccept(restaurantId, LocalDateTime.of(2024, 1, 1, 12, 0)));
    }

    @Test
    void rejectsWhenRestaurantClosed() {
        Restaurant r = activeRestaurant("Mon-Sun 11:00-23:00");
        r.close();
        given(restaurantRepository.getById(new RestaurantId(restaurantId))).willReturn(Optional.of(r));

        DomainConflictException ex = assertThrows(DomainConflictException.class,
                () -> sut.verifyCanAccept(restaurantId, LocalDateTime.of(2024, 1, 1, 12, 0)));
        assertTrue(ex.getMessage().contains("Restaurant closed"));
    }

    @Test
    void rejectsWhenPrepWouldPassClosingTime() {
        Restaurant r = activeRestaurant("Mon-Sun 11:00-23:00");
        given(restaurantRepository.getById(new RestaurantId(restaurantId))).willReturn(Optional.of(r));
        given(prepTimeEstimator.estimate()).willReturn(Duration.ofMinutes(30));

        // 22:45 + 30min = 23:15 → after 23:00 close
        DomainConflictException ex = assertThrows(DomainConflictException.class,
                () -> sut.verifyCanAccept(restaurantId, LocalDateTime.of(2024, 1, 1, 22, 45)));
        assertTrue(ex.getMessage().contains("before closing time"));
    }
}
