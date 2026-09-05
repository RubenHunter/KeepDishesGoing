package be.kdg.backend.domain;

import be.kdg.backend.domain.dish.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DishTest {
    private static Price eur(String amount) {
        return new Price(new BigDecimal(amount), "EUR");
    }

    @Test
    void createDraft_shouldHaveGivenFieldsAndDraftStatus() {
        // Arrange
        DishName name = new DishName("Tea");
        Description desc = new Description("Hot");

        // Act
        Dish d = Dish.createDraft(name, desc, eur("2.00"), DishCategory.BEVERAGE, null);

        // Assert
        assertNotNull(d.getId());
        assertEquals("Tea", d.getName().name());
        assertEquals("Hot", d.getDescription().description());
        assertEquals(new BigDecimal("2.00"), d.getPrice().amount());
        assertEquals(DishCategory.BEVERAGE, d.getCategory());
        assertEquals(DishStatus.DRAFT, d.getStatus());
    }

    @Test
    void publish_shouldBecomePublished() {
        // Arrange
        Dish d = Dish.createDraft(new DishName("Soup"), new Description("Tomato"), eur("4.00"), DishCategory.APPETIZER, null);

        // Act
        d.publish();

        // Assert
        assertEquals(DishStatus.PUBLISHED, d.getStatus());
    }

    @Test
    void markAsDraft_fromPublished_shouldBecomeDraft() {
        // Arrange
        Dish d = Dish.createDraft(new DishName("Coffee"), new Description("Black"), eur("2.50"), DishCategory.BEVERAGE, null);
        d.publish();

        // Act
        d.markAsDraft();

        // Assert
        assertEquals(DishStatus.DRAFT, d.getStatus());
    }

    @Test
    void markOutOfStock_shouldBecomeOutOfStock() {
        // Arrange
        Dish d = Dish.createDraft(new DishName("Fries"), new Description("Salt"), eur("3.00"), DishCategory.APPETIZER, null);
        d.publish();

        // Act
        d.markOutOfStock();

        // Assert
        assertEquals(DishStatus.OUT_OF_STOCK, d.getStatus());
    }

    @Test
    void updateFields_shouldChangeDescriptionPriceAndCategory() {
        // Arrange
        Dish d = Dish.createDraft(new DishName("Tea"), new Description("Hot"), eur("2.00"), DishCategory.BEVERAGE, null);

        // Act
        d.updateDescription(new Description("Iced"));
        d.updatePrice(eur("2.20"));
        d.updateCategory(DishCategory.DESSERT);

        // Assert
        assertEquals("Iced", d.getDescription().description());
        assertEquals(new BigDecimal("2.20"), d.getPrice().amount());
        assertEquals(DishCategory.DESSERT, d.getCategory());
    }
}
