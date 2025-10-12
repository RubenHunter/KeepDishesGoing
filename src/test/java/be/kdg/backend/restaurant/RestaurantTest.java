package be.kdg.backend.restaurant;

import be.kdg.backend.domain.Price;
import be.kdg.backend.domain.dish.Description;
import be.kdg.backend.domain.dish.Dish;
import be.kdg.backend.domain.dish.DishCategory;
import be.kdg.backend.domain.dish.DishId;
import be.kdg.backend.domain.dish.DishStatus;
import be.kdg.backend.domain.dish.DishName;
import be.kdg.backend.domain.restaurant.Restaurant;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RestaurantTest {

    private static Price eur(String amount) {
        return new Price(new BigDecimal(amount), "EUR");
    }

    @Test
    void shouldCreateDraftAndPublish() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        // Act
        DishId id = r.createDraftDish(
                new DishName("Pasta"),
                new Description("Tasty"),
                DishCategory.MAIN_COURSE,
                eur("10.00")
        );
        // Assert
        Dish draft = r.getDishById(id);
        assertEquals(DishStatus.DRAFT, draft.getStatus());
        assertTrue(r.getPublishedMenu().isEmpty());

        // Act
        r.publishDish(id);
        // Assert
        Dish published = r.getDishById(id);
        assertEquals(DishStatus.PUBLISHED, published.getStatus());
        assertEquals(1, r.getPublishedMenu().size());
    }

    @Test
    void shouldUpdateDraftInPlace() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        DishId id = r.createDraftDish(
                new DishName("Soup"),
                new Description("Tomato"),
                DishCategory.APPETIZER,
                eur("4.00")
        );
        // Act
        DishId same = r.updateDraftDish(id, "Soup", "Tomato basil", eur("4.50"), DishCategory.APPETIZER);
        // Assert
        assertEquals(id, same);
        Dish dish = r.getDishById(id);
        assertEquals(DishStatus.DRAFT, dish.getStatus());
        assertEquals(new BigDecimal("4.50"), dish.getPrice().amount());
        assertEquals("Tomato basil", dish.getDescription().description());
        assertTrue(r.getPublishedMenu().isEmpty());
    }

    @Test
    void shouldReuseSingleDraftWhenUpdatingPublished() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        DishId pubId = r.createDraftDish(new DishName("Burger"), new Description("A"), DishCategory.MAIN_COURSE, eur("8.00"));
        r.publishDish(pubId);

        // Act
        DishId draft1 = r.updateDraftDish(pubId, "Burger", "A+", eur("8.50"), DishCategory.MAIN_COURSE);
        DishId draft2 = r.updateDraftDish(pubId, "Burger", "A++", eur("9.00"), DishCategory.MAIN_COURSE);

        // Assert
        assertEquals(draft1, draft2);
        long drafts = r.getDishes().stream()
                .filter(d -> d.getName().name().equals("Burger") && d.getStatus() == DishStatus.DRAFT)
                .count();
        assertEquals(1, drafts);
        assertEquals(1, r.getPublishedMenu().size());
    }

    @Test
    void shouldReplacePublishedVersionOnPublish() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        DishId pubId = r.createDraftDish(new DishName("Pizza"), new Description("Margarita"), DishCategory.MAIN_COURSE, eur("7.00"));
        r.publishDish(pubId);
        // Assert
        assertEquals(1, r.getPublishedMenu().size());

        // Act
        DishId draftId = r.updateDraftDish(pubId, "Pizza", "Margarita+", eur("8.00"), DishCategory.MAIN_COURSE);
        Dish draft = r.getDishById(draftId);
        // Assert
        assertEquals(DishStatus.DRAFT, draft.getStatus());

        // Act
        r.publishDish(draftId);

        // Assert
        List<Dish> menu = r.getPublishedMenu();
        assertEquals(1, menu.size());
        assertEquals("Pizza", menu.getFirst().getName().name());
        assertEquals(new BigDecimal("8.00"), menu.getFirst().getPrice().amount());

        long publishedCountWithName = r.getDishes().stream()
                .filter(d -> d.getName().name().equals("Pizza") && d.getStatus() == DishStatus.PUBLISHED)
                .count();
        assertEquals(1, publishedCountWithName);
    }

    @Test
    void shouldDepublishAndToggleAvailability() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        DishId id = r.createDraftDish(new DishName("Salad"), new Description("Green"), DishCategory.MAIN_COURSE, eur("5.00"));
        r.publishDish(id);
        // Assert
        assertEquals(1, r.getPublishedMenu().size());

        // Act
        r.dePublishDish(id);
        // Assert
        assertEquals(DishStatus.DRAFT, r.getDishById(id).getStatus());
        assertTrue(r.getPublishedMenu().isEmpty());

        // Act
        r.markDishOutOfStock(id);
        // Assert
        assertEquals(DishStatus.OUT_OF_STOCK, r.getDishById(id).getStatus());
        assertTrue(r.getPublishedMenu().isEmpty());
    }

    @Test
    void shouldNotAllowNameChangeOnUpdate() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        DishId id = r.createDraftDish(new DishName("Taco"), new Description("Beef"), DishCategory.MAIN_COURSE, eur("6.00"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> r.updateDraftDish(id, "Burrito", "Beef", eur("6.00"), DishCategory.MAIN_COURSE));
    }

    @Test
    void shouldValidatePublishConstraints() {
        // Arrange
        Restaurant r = Restaurant.create("Resto");
        DishId id = r.createDraftDish(new DishName("Tea"), new Description("Hot"), DishCategory.BEVERAGE, eur("2.00"));
        r.publishDish(id);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> r.publishDish(id));
    }
}
