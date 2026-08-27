package be.kdg.backend.application;

import be.kdg.backend.api.dto.UpdateDishDto;
import be.kdg.backend.domain.NotFoundException;
import be.kdg.backend.domain.Price;
import be.kdg.backend.domain.dish.Description;
import be.kdg.backend.domain.dish.Dish;
import be.kdg.backend.domain.dish.DishCategory;
import be.kdg.backend.api.dto.DishDto;
import be.kdg.backend.domain.dish.DishId;
import be.kdg.backend.domain.dish.DishName;
import be.kdg.backend.domain.dish.DishStatus;
import be.kdg.backend.domain.restaurant.IRestaurantRepository;
import be.kdg.backend.domain.restaurant.Restaurant;
import be.kdg.backend.domain.restaurant.RestaurantId;
import be.kdg.backend.domain.scheduling.IScheduledPublishRepository;
import be.kdg.backend.domain.scheduling.ScheduledPublishJob;
import be.kdg.backend.infrastructure.EntityNotFoundException;
import be.kdg.backend.infrastructure.jpa.JpaScheduledPublishEntity;
import be.kdg.backend.infrastructure.jpa.JpaScheduledPublishRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DishServiceIntegrationTest {
    @Mock
    private IRestaurantRepository restaurantRepository;

    @Mock
    private IScheduledPublishRepository scheduledRepo;

    private DishService sut;

    @BeforeEach
    void setUp() {
        sut = new DishService(restaurantRepository, scheduledRepo, 0.01);
    }

    private static Price eur(String amount) {
        return new Price(new BigDecimal(amount), "EUR");
    }

    @Test
    void shouldCreateDraftDishAndPersist() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        RestaurantId rid = r.getId();
        given(restaurantRepository.getById(rid)).willReturn(Optional.of(r));

        // Act
        DishId id = sut.createDraftDish(
                rid,
                new DishName("Pasta"),
                new Description("Fresh"),
                eur("9.50"),
                DishCategory.MAIN_COURSE,
                null
        );

        // Assert
        assertNotNull(id);
        assertEquals(1, r.getDishes().size());
        assertEquals(DishStatus.DRAFT, r.getDishes().getFirst().getStatus());
        verify(restaurantRepository, times(1)).save(r);
    }

    @Test
    void shouldThrowWhenRestaurantMissingOnCreate() {
        // Arrange
        RestaurantId rid = RestaurantId.create();
        given(restaurantRepository.getById(rid)).willReturn(Optional.empty());

        // Act
        // Assert
        assertThrows(NotFoundException.class, () ->
                sut.createDraftDish(rid, new DishName("Soup"), new Description("Tomato"), eur("4.00"), DishCategory.APPETIZER, null));
    }

    @Test
    void shouldListAllDishesOfRestaurant() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        RestaurantId rid = r.getId();
        r.createDraftDish(new DishName("Salad"), new Description("Green"), DishCategory.APPETIZER, eur("5.00"), null);
        r.createDraftDish(new DishName("Burger"), new Description("Beef"), DishCategory.MAIN_COURSE, eur("12.00"), null);
        given(restaurantRepository.getById(rid)).willReturn(Optional.of(r));

        // Act
        List<Dish> all = sut.listDishesOfRestaurant(rid);

        // Assert
        assertEquals(2, all.size());
    }

    @Test
    void shouldGetDishByIdViaLookup() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        DishId id = r.createDraftDish(new DishName("Tea"), new Description("Hot"), DishCategory.BEVERAGE, eur("2.50"), null);
        given(restaurantRepository.findByDishId(id)).willReturn(Optional.of(r));

        // Act
        Dish found = sut.getDishById(id);

        // Assert
        assertEquals(id, found.getId());
        assertEquals("Tea", found.getName().name());
    }

    @Test
    void shouldUpdateDraftDishInPlace() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        RestaurantId rid = r.getId();
        DishId id = r.createDraftDish(new DishName("Taco"), new Description("Beef"), DishCategory.MAIN_COURSE, eur("5.00"), null);
        given(restaurantRepository.getById(rid)).willReturn(Optional.of(r));
        UpdateDishDto dto = new UpdateDishDto("Taco", "Spicy beef", eur("6.00"), DishCategory.MAIN_COURSE, null);

        // Act
        DishDto updated = sut.updateDraftDish(rid, id, dto);

        // Assert
        assertEquals(id.id(), updated.id());
        assertEquals("Spicy beef", updated.description());
        assertEquals("DRAFT", updated.status());
        verify(restaurantRepository, times(1)).save(r);
    }

    @Test
    void shouldCreateOrReuseDraftWhenUpdatingPublished() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        RestaurantId rid = r.getId();
        DishId publishedId = r.createDraftDish(new DishName("Pizza"), new Description("Margarita"), DishCategory.MAIN_COURSE, eur("8.00"), null);
        r.publishDish(publishedId);
        given(restaurantRepository.getById(rid)).willReturn(Optional.of(r));
        UpdateDishDto dto = new UpdateDishDto("Pizza", "Margarita XL", eur("9.50"), DishCategory.MAIN_COURSE, null);

        // Act
        DishDto draftDto = sut.updateDraftDish(rid, publishedId, dto);

        // Assert
        assertEquals("Pizza", draftDto.name());
        assertEquals("Margarita XL", draftDto.description());
        assertEquals("DRAFT", draftDto.status());
        assertNotEquals(publishedId.id(), draftDto.id());
        verify(restaurantRepository, times(1)).save(r);
    }

    @Test
    void shouldPublishDishAndReplaceOlderPublishedWithSameName() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        RestaurantId rid = r.getId();
        DishId oldId = r.createDraftDish(new DishName("Pizza"), new Description("Old"), DishCategory.MAIN_COURSE, eur("8.00"), null);
        r.publishDish(oldId);
        DishId newDraftId = r.createDraftDish(new DishName("Pizza"), new Description("New"), DishCategory.MAIN_COURSE, eur("9.00"), null);
        given(restaurantRepository.getById(rid)).willReturn(Optional.of(r));

        // Act
        sut.publishDish(rid, newDraftId);

        // Assert
        assertEquals(1, r.getPublishedMenu().size());
        assertEquals(DishStatus.PUBLISHED, r.getDishById(newDraftId).getStatus());
        verify(restaurantRepository, times(1)).save(r);
    }

    @Test
    void shouldDepublishDish() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        RestaurantId rid = r.getId();
        DishId id = r.createDraftDish(new DishName("Soup"), new Description("Tomato"), DishCategory.APPETIZER, eur("4.00"), null);
        r.publishDish(id);
        given(restaurantRepository.getById(rid)).willReturn(Optional.of(r));

        // Act
        sut.dePublishDish(rid, id);

        // Assert
        assertEquals(DishStatus.DRAFT, r.getDishById(id).getStatus());
        verify(restaurantRepository, times(1)).save(r);
    }

    @Test
    void shouldToggleAvailability() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        RestaurantId rid = r.getId();
        DishId id = r.createDraftDish(new DishName("Tea"), new Description("Hot"), DishCategory.BEVERAGE, eur("2.00"), null);
        r.publishDish(id);
        given(restaurantRepository.getById(rid)).willReturn(Optional.of(r));

        // Act
        sut.setDishAvailability(rid, id, false);

        // Assert
        assertEquals(DishStatus.OUT_OF_STOCK, r.getDishById(id).getStatus());
        verify(restaurantRepository, times(1)).save(r);
    }

    @Test
    void shouldPublishAllDraftDishes() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        RestaurantId rid = r.getId();
        r.createDraftDish(new DishName("DishA"), new Description(""), DishCategory.MAIN_COURSE, eur("3.00"), null);
        r.createDraftDish(new DishName("DishB"), new Description(""), DishCategory.MAIN_COURSE, eur("4.00"), null);
        given(restaurantRepository.getById(rid)).willReturn(Optional.of(r));

        // Act
        sut.publishAllDraftDishes(rid);

        // Assert
        assertEquals(2, r.getPublishedMenu().size());
        verify(restaurantRepository, times(1)).save(r);
    }

    @Test
    void shouldSchedulePublishAllDraftDishes() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        RestaurantId rid = r.getId();
        LocalDateTime future = LocalDateTime.now().plusHours(1);
        given(restaurantRepository.getById(rid)).willReturn(Optional.of(r));
        ArgumentCaptor<ScheduledPublishJob> captor = ArgumentCaptor.forClass(ScheduledPublishJob.class);

        // Act
        sut.schedulePublishAllDraftDishes(rid, future);

        // Assert
        verify(scheduledRepo, times(1)).save(captor.capture());
        ScheduledPublishJob job = captor.getValue();
        assertEquals(rid.id(), job.getRestaurantId());
        assertEquals(future, job.getPublishAt());
        assertEquals(ScheduledPublishJob.Status.PENDING, job.getStatus());
    }

    @Test
    void shouldRejectInvalidScheduleTimes() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        RestaurantId rid = r.getId();
        given(restaurantRepository.getById(rid)).willReturn(Optional.of(r));

        // Act
        // Assert
        assertThrows(IllegalArgumentException.class, () -> sut.schedulePublishAllDraftDishes(rid, null));
        assertThrows(IllegalArgumentException.class, () -> sut.schedulePublishAllDraftDishes(rid, LocalDateTime.now().minusMinutes(1)));
        verify(scheduledRepo, never()).save(any());
    }
}
