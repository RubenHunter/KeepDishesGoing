package be.kdg.backend.application;

import be.kdg.backend.api.dto.UpdateDishDto;
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
import be.kdg.backend.infrastructure.EntityNotFoundException;
import be.kdg.backend.infrastructure.jpa.JpaScheduledPublishEntity;
import be.kdg.backend.infrastructure.jpa.JpaScheduledPublishRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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
    private JpaScheduledPublishRepository scheduledRepo;

    @InjectMocks
    private DishService sut;

    private static Price eur(String amount) {
        return new Price(new BigDecimal(amount), "EUR");
    }

    @Test
    void shouldCreateDraftDishAndPersist() {
        Restaurant r = Restaurant.create("Resto");
        RestaurantId rid = r.getId();

        given(restaurantRepository.getById(rid)).willReturn(Optional.of(r));

        DishId id = sut.createDraftDish(
                rid,
                new DishName("Pasta"),
                new Description("Fresh"),
                eur("9.50"),
                DishCategory.MAIN_COURSE
        );

        assertNotNull(id);
        assertEquals(1, r.getDishes().size());
        Dish created = r.getDishes().getFirst();
        assertEquals(DishStatus.DRAFT, created.getStatus());
        assertEquals("Pasta", created.getName().name());
        verify(restaurantRepository, times(1)).save(r);
    }

    @Test
    void shouldThrowWhenRestaurantMissingOnCreate() {
        RestaurantId rid = RestaurantId.create();
        given(restaurantRepository.getById(rid)).willReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () ->
                sut.createDraftDish(rid, new DishName("Soup"), new Description("Tomato"), eur("4.00"), DishCategory.APPETIZER));
    }

    @Test
    void shouldListAllDishesOfRestaurant() {
        Restaurant r = Restaurant.create("Resto");
        RestaurantId rid = r.getId();
        r.createDraftDish(new DishName("Salad"), new Description("Green"), DishCategory.APPETIZER, eur("5.00"));
        r.createDraftDish(new DishName("Burger"), new Description("Beef"), DishCategory.MAIN_COURSE, eur("12.00"));

        given(restaurantRepository.getById(rid)).willReturn(Optional.of(r));

        List<Dish> all = sut.listDishesOfRestaurant(rid);
        assertEquals(2, all.size());
    }

    @Test
    void shouldGetDishByIdViaLookup() {
        Restaurant r = Restaurant.create("Resto");
        DishId id = r.createDraftDish(new DishName("Tea"), new Description("Hot"), DishCategory.BEVERAGE, eur("2.50"));
        given(restaurantRepository.findByDishId(id)).willReturn(Optional.of(r));

        Dish found = sut.getDishById(id);
        assertEquals(id, found.getId());
        assertEquals("Tea", found.getName().name());
    }

    @Test
    void shouldUpdateDraftDishInPlace() {
        Restaurant r = Restaurant.create("Resto");
        RestaurantId rid = r.getId();
        DishId id = r.createDraftDish(new DishName("Taco"), new Description("Beef"), DishCategory.MAIN_COURSE, eur("5.00"));

        given(restaurantRepository.getById(rid)).willReturn(Optional.of(r));

        UpdateDishDto dto = new UpdateDishDto("Taco", "Spicy beef", eur("6.00"), DishCategory.MAIN_COURSE);
        var updated = sut.updateDraftDish(rid, id, dto);

        assertEquals(id.id(), updated.id());
        assertEquals("Taco", updated.name());
        assertEquals("Spicy beef", updated.description());
        assertEquals("DRAFT", updated.status());
        verify(restaurantRepository, times(1)).save(r);
    }

    @Test
    void shouldCreateOrReuseDraftWhenUpdatingPublished() {
        Restaurant r = Restaurant.create("Resto");
        RestaurantId rid = r.getId();
        DishId publishedId = r.createDraftDish(new DishName("Pizza"), new Description("Margarita"), DishCategory.MAIN_COURSE, eur("8.00"));
        r.publishDish(publishedId);

        given(restaurantRepository.getById(rid)).willReturn(Optional.of(r));

        UpdateDishDto dto = new UpdateDishDto("Pizza", "Margarita XL", eur("9.50"), DishCategory.MAIN_COURSE);
        var draftDto = sut.updateDraftDish(rid, publishedId, dto);

        assertEquals("Pizza", draftDto.name());
        assertEquals("Margarita XL", draftDto.description());
        assertEquals("DRAFT", draftDto.status());
        assertNotEquals(publishedId.id(), draftDto.id());
        verify(restaurantRepository, times(1)).save(r);
    }

    @Test
    void shouldPublishDishAndReplaceOlderPublishedWithSameName() {
        Restaurant r = Restaurant.create("Resto");
        RestaurantId rid = r.getId();
        // Old published
        DishId oldId = r.createDraftDish(new DishName("Pizza"), new Description("Old"), DishCategory.MAIN_COURSE, eur("8.00"));
        r.publishDish(oldId);
        // New draft same logical dish
        DishId newDraftId = r.createDraftDish(new DishName("Pizza"), new Description("New"), DishCategory.MAIN_COURSE, eur("9.00"));

        given(restaurantRepository.getById(rid)).willReturn(Optional.of(r));

        sut.publishDish(rid, newDraftId);

        Dish oldDish = r.getDishes().stream().filter(d -> d.getId().equals(oldId)).findFirst().orElseThrow();
        Dish newDish = r.getDishes().stream().filter(d -> d.getId().equals(newDraftId)).findFirst().orElseThrow();

        assertEquals(DishStatus.DRAFT, oldDish.getStatus());
        assertEquals(DishStatus.PUBLISHED, newDish.getStatus());
        assertEquals(1, r.getPublishedMenu().size());
        verify(restaurantRepository, times(1)).save(r);
    }

    @Test
    void shouldDepublishDish() {
        Restaurant r = Restaurant.create("Resto");
        RestaurantId rid = r.getId();
        DishId id = r.createDraftDish(new DishName("Soup"), new Description("Tomato"), DishCategory.APPETIZER, eur("4.00"));
        r.publishDish(id);

        given(restaurantRepository.getById(rid)).willReturn(Optional.of(r));

        sut.dePublishDish(rid, id);
        assertEquals(DishStatus.DRAFT, r.getDishById(id).getStatus());
        verify(restaurantRepository, times(1)).save(r);
    }

    @Test
    void shouldToggleAvailability() {
        Restaurant r = Restaurant.create("Resto");
        RestaurantId rid = r.getId();
        DishId id = r.createDraftDish(new DishName("Tea"), new Description("Hot"), DishCategory.BEVERAGE, eur("2.00"));
        r.publishDish(id);
        given(restaurantRepository.getById(rid)).willReturn(Optional.of(r));

        sut.setDishAvailability(rid, id, false);
        assertEquals(DishStatus.OUT_OF_STOCK, r.getDishById(id).getStatus());

        sut.setDishAvailability(rid, id, true);
        assertEquals(DishStatus.PUBLISHED, r.getDishById(id).getStatus());
        verify(restaurantRepository, times(2)).save(r);
    }

    @Test
    void shouldPublishAllDraftDishes() {
        Restaurant r = Restaurant.create("Resto");
        RestaurantId rid = r.getId();
        DishId d1 = r.createDraftDish(new DishName("A"), new Description(""), DishCategory.MAIN_COURSE, eur("3.00"));
        DishId d2 = r.createDraftDish(new DishName("B"), new Description(""), DishCategory.MAIN_COURSE, eur("4.00"));
        assertEquals(DishStatus.DRAFT, r.getDishById(d1).getStatus());
        assertEquals(DishStatus.DRAFT, r.getDishById(d2).getStatus());

        given(restaurantRepository.getById(rid)).willReturn(Optional.of(r));

        sut.publishAllDraftDishes(rid);

        assertEquals(2, r.getPublishedMenu().size());
        verify(restaurantRepository, times(1)).save(r);
    }

    @Test
    void shouldSchedulePublishAllDraftDishes() {
        Restaurant r = Restaurant.create("Resto");
        RestaurantId rid = r.getId();
        LocalDateTime future = LocalDateTime.now().plusHours(1);

        given(restaurantRepository.getById(rid)).willReturn(Optional.of(r));

        ArgumentCaptor<JpaScheduledPublishEntity> captor = ArgumentCaptor.forClass(JpaScheduledPublishEntity.class);

        sut.schedulePublishAllDraftDishes(rid, future);

        verify(scheduledRepo, times(1)).save(captor.capture());
        JpaScheduledPublishEntity job = captor.getValue();
        assertEquals(rid.id(), job.getRestaurantId());
        assertEquals(future, job.getPublishAt());
        assertEquals(JpaScheduledPublishEntity.Status.PENDING, job.getStatus());
    }

    @Test
    void shouldRejectInvalidScheduleTimes() {
        Restaurant r = Restaurant.create("Resto");
        RestaurantId rid = r.getId();
        given(restaurantRepository.getById(rid)).willReturn(Optional.of(r));

        assertThrows(IllegalArgumentException.class, () -> sut.schedulePublishAllDraftDishes(rid, null));
        assertThrows(IllegalArgumentException.class, () ->
                sut.schedulePublishAllDraftDishes(rid, LocalDateTime.now().minusMinutes(1)));
        verify(scheduledRepo, never()).save(any());
    }
}
