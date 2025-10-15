package be.kdg.backend.application;

import be.kdg.backend.domain.NotFoundException;
import be.kdg.backend.domain.restaurant.IRestaurantRepository;
import be.kdg.backend.domain.restaurant.Restaurant;
import be.kdg.backend.domain.restaurant.RestaurantId;
import be.kdg.backend.domain.restaurant.RestaurantStatus;
import be.kdg.backend.infrastructure.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceIntegrationTest {
    @Mock
    private IRestaurantRepository restaurantRepository;

    @InjectMocks
    private RestaurantService sut;

    @Test
    void shouldCreateRestaurantAndPersist() {
        // Arrange
        ArgumentCaptor<Restaurant> captor = ArgumentCaptor.forClass(Restaurant.class);

        // Act
        RestaurantId created = sut.createRestaurant("New Resto");

        // Assert
        verify(restaurantRepository, times(1)).save(captor.capture());
        Restaurant saved = captor.getValue();
        assertNotNull(created);
        assertNotNull(saved);
        assertEquals("New Resto", saved.getName().name());
        assertEquals(RestaurantStatus.INACTIVE, saved.getStatus());
        assertEquals(saved.getId(), created);
    }

    @Test
    void shouldListRestaurants() {
        // Arrange
        given(restaurantRepository.getAll()).willReturn(List.of(Restaurant.create("A"), Restaurant.create("B")));

        // Act
        var all = sut.listRestaurants();

        // Assert
        assertEquals(2, all.size());
    }

    @Test
    void shouldGetRestaurantById() {
        // Arrange
        Restaurant r = Restaurant.create("R");
        RestaurantId id = r.getId();
        given(restaurantRepository.getById(id)).willReturn(Optional.of(r));

        // Act
        Restaurant found = sut.getRestaurantById(id);

        // Assert
        assertEquals(r, found);
    }

    @Test
    void shouldThrowWhenRestaurantMissingOnGet() {
        // Arrange
        RestaurantId id = RestaurantId.create();
        given(restaurantRepository.getById(id)).willReturn(Optional.empty());

        // Act
        // Assert
        assertThrows(NotFoundException.class, () -> sut.getRestaurantById(id));
    }

    @Test
    void shouldOpenRestaurant() {
        // Arrange
        Restaurant r = Restaurant.create("R");
        RestaurantId id = r.getId();
        given(restaurantRepository.getById(id)).willReturn(Optional.of(r));

        // Act
        sut.openRestaurant(id);

        // Assert
        assertEquals(RestaurantStatus.ACTIVE, r.getStatus());
        verify(restaurantRepository, times(1)).save(r);
    }

    @Test
    void openingAlreadyOpenShouldThrow() {
        // Arrange
        Restaurant r = Restaurant.create("R");
        r.open();
        RestaurantId id = r.getId();
        given(restaurantRepository.getById(id)).willReturn(Optional.of(r));

        // Act
        // Assert
        assertThrows(IllegalStateException.class, () -> sut.openRestaurant(id));
        verify(restaurantRepository, never()).save(any());
    }

    @Test
    void shouldCloseRestaurant() {
        // Arrange
        Restaurant r = Restaurant.create("R");
        r.open();
        RestaurantId id = r.getId();
        given(restaurantRepository.getById(id)).willReturn(Optional.of(r));

        // Act
        sut.closeRestaurant(id);

        // Assert
        assertEquals(RestaurantStatus.INACTIVE, r.getStatus());
        verify(restaurantRepository, times(1)).save(r);
    }

    @Test
    void closingAlreadyClosedShouldThrow() {
        // Arrange
        Restaurant r = Restaurant.create("R");
        RestaurantId id = r.getId();
        given(restaurantRepository.getById(id)).willReturn(Optional.of(r));

        // Act
        // Assert
        assertThrows(IllegalStateException.class, () -> sut.closeRestaurant(id));
        verify(restaurantRepository, never()).save(any());
    }
}