package be.kdg.backend.domain;

import be.kdg.backend.domain.dish.Description;
import be.kdg.backend.domain.dish.Dish;
import be.kdg.backend.domain.dish.DishCategory;
import be.kdg.backend.domain.dish.DishId;
import be.kdg.backend.domain.dish.DishName;
import be.kdg.backend.domain.dish.DishStatus;
import be.kdg.backend.domain.restaurant.Restaurant;
import be.kdg.backend.domain.restaurant.RestaurantStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RestaurantTest {
    private static Price eur(String amount) {
        return new Price(new BigDecimal(amount), "EUR");
    }

    @Test
    void create_shouldStartInactiveWithNoDishes() {
        // Arrange
        // Act
        Restaurant r = Restaurant.create("Resto");

        // Assert
        assertEquals(RestaurantStatus.INACTIVE, r.getStatus());
        assertTrue(r.getDishes().isEmpty());
    }

    @Test
    void open_close_transitionsAndGuards() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");

        // Act
        r.open();

        // Assert
        assertEquals(RestaurantStatus.ACTIVE, r.getStatus());

        // Arrange
        // Act
        r.close();

        // Assert
        assertEquals(RestaurantStatus.INACTIVE, r.getStatus());
        assertThrows(IllegalStateException.class, r::close);
        r.open();
        assertThrows(IllegalStateException.class, r::open);
    }

    @Test
    void createDraftDish_shouldAddDraft() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");

        // Act
        DishId id = r.createDraftDish(new DishName("Pasta"), new Description("Fresh"), DishCategory.MAIN_COURSE, eur("9.50"));

        // Assert
        assertNotNull(id);
        Dish d = r.getDishById(id);
        assertEquals(DishStatus.DRAFT, d.getStatus());
        assertEquals("Pasta", d.getName().name());
        assertEquals(new BigDecimal("9.50"), d.getPrice().amount());
    }

    @Test
    void updateDraftDish_inPlace_onDraft() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        DishId id = r.createDraftDish(new DishName("Burger"), new Description("Beef"), DishCategory.MAIN_COURSE, eur("12.00"));

        // Act
        DishId result = r.updateDraftDish(id, "Burger", "Beef+Cheese", eur("13.00"), DishCategory.MAIN_COURSE);

        // Assert
        assertEquals(id, result);
        Dish d = r.getDishById(id);
        assertEquals("Beef+Cheese", d.getDescription().description());
        assertEquals(new BigDecimal("13.00"), d.getPrice().amount());
        assertEquals(DishStatus.DRAFT, d.getStatus());
    }

    @Test
    void updateDraftDish_fromPublished_createsNewDraft() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        DishId pubId = r.createDraftDish(new DishName("Pizza"), new Description("Margarita"), DishCategory.MAIN_COURSE, eur("8.00"));
        r.publishDish(pubId);

        // Act
        DishId draftId = r.updateDraftDish(pubId, "Pizza", "Margarita XL", eur("9.50"), DishCategory.MAIN_COURSE);

        // Assert
        assertNotEquals(pubId, draftId);
        Dish draft = r.getDishById(draftId);
        Dish published = r.getDishById(pubId);
        assertEquals("Margarita XL", draft.getDescription().description());
        assertEquals(DishStatus.DRAFT, draft.getStatus());
        assertEquals("Margarita", published.getDescription().description());
        assertEquals(DishStatus.PUBLISHED, published.getStatus());
    }

    @Test
    void updateDraftDish_fromPublished_reusesExistingDraft() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        DishName name = new DishName("Sushi");
        DishId pubId = r.createDraftDish(name, new Description("Classic"), DishCategory.MAIN_COURSE, eur("10.00"));
        r.publishDish(pubId);
        DishId existingDraft = r.createDraftDish(name, new Description("Scratch"), DishCategory.MAIN_COURSE, eur("11.00"));

        // Act
        DishId result = r.updateDraftDish(pubId, "Sushi", "Classic XL", eur("12.00"), DishCategory.MAIN_COURSE);

        // Assert
        assertEquals(existingDraft, result);
        Dish draft = r.getDishById(existingDraft);
        assertEquals("Classic XL", draft.getDescription().description());
        assertEquals(new BigDecimal("12.00"), draft.getPrice().amount());
        assertEquals(DishStatus.DRAFT, draft.getStatus());
    }

    @Test
    void publishDish_shouldReplaceOlderPublishedWithSameName() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        DishName name = new DishName("Pizza");
        DishId oldId = r.createDraftDish(name, new Description("Old"), DishCategory.MAIN_COURSE, eur("7.00"));
        r.publishDish(oldId);
        DishId newId = r.createDraftDish(name, new Description("New"), DishCategory.MAIN_COURSE, eur("8.00"));

        // Act
        r.publishDish(newId);

        // Assert
        List<Dish> menu = r.getPublishedMenu();
        assertEquals(1, menu.size());
        assertEquals("Pizza", menu.getFirst().getName().name());
        assertEquals(new BigDecimal("8.00"), menu.getFirst().getPrice().amount());
        long publishedCountWithName = r.getDishes().stream()
                .filter(d -> d.getName().equals(name) && d.getStatus() == DishStatus.PUBLISHED)
                .count();
        assertEquals(1, publishedCountWithName);
    }

    @Test
    void dePublish_and_markOutOfStock() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        DishId id = r.createDraftDish(new DishName("Soup"), new Description("Tomato"), DishCategory.APPETIZER, eur("4.00"));
        r.publishDish(id);

        // Act
        r.dePublishDish(id);

        // Assert
        assertEquals(DishStatus.DRAFT, r.getDishById(id).getStatus());

        // Arrange
        r.publishDish(id);

        // Act
        r.markDishOutOfStock(id);

        // Assert
        assertEquals(DishStatus.OUT_OF_STOCK, r.getDishById(id).getStatus());
    }

    @Test
    void getPublishedMenu_shouldReturnOnlyPublished() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        DishId d1 = r.createDraftDish(new DishName("DishA"), new Description(""), DishCategory.MAIN_COURSE, eur("5.00"));
        DishId d2 = r.createDraftDish(new DishName("DishB"), new Description(""), DishCategory.MAIN_COURSE, eur("6.00"));
        r.publishDish(d2);

        // Act
        List<Dish> menu = r.getPublishedMenu();

        // Assert
        assertEquals(1, menu.size());
        assertEquals(d2, menu.getFirst().getId());
    }

    @Test
    void shouldNotAllowNameChangeOnUpdate() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        DishId id = r.createDraftDish(new DishName("Tea"), new Description("Hot"), DishCategory.BEVERAGE, eur("2.00"));

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () ->
                r.updateDraftDish(id, "Coffee", "Hot", eur("2.50"), DishCategory.BEVERAGE));
    }

    @Test
    void shouldValidatePublishConstraints() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        DishId id = r.createDraftDish(new DishName("Tea"), new Description("Hot"), DishCategory.BEVERAGE, eur("2.00"));
        r.publishDish(id);

        // Act + Assert
        assertThrows(IllegalStateException.class, () -> r.publishDish(id));
    }
}
