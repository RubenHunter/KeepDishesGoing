package be.kdg.backend.api;

import be.kdg.backend.TestHelper;
import be.kdg.backend.application.ScheduledPublishProcessor;
import be.kdg.backend.domain.Price;
import be.kdg.backend.domain.dish.*;
import be.kdg.backend.domain.restaurant.Restaurant;
import be.kdg.backend.domain.restaurant.RestaurantId;
import be.kdg.backend.infrastructure.jpa.JpaScheduledPublishEntity;
import be.kdg.backend.infrastructure.jpa.JpaScheduledPublishRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DishControllerIntegrationTest {

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleIllegalState(IllegalStateException ex) {
        return ex.getMessage();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestHelper helper;

    @Autowired
    private JpaScheduledPublishRepository scheduledRepo;

    @Autowired
    private ScheduledPublishProcessor scheduledProcessor;

    @AfterEach
    void tearDown() {
        helper.cleanUp();
    }

    // US4: Draft should not affect live menu
    @Test
    void us4_draftDoesNotAffectMenu() throws Exception {
        // Arrange
        Restaurant r = helper.createRestaurant("US4");
        RestaurantId rid = helper.id(r);
        String payload = """
        {
          "name": "Tagliatelle",
          "description": "Mushroom sauce",
          "price": { "amount": 13.0, "currency": "EUR" },
          "category": "MAIN_COURSE"
        }
        """;

        // Act
        var result = mockMvc.perform(
                post("/api/restaurants/{id}/dishes", rid.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload));

        // Assert
        result.andExpect(status().isCreated());
        mockMvc.perform(get("/api/restaurants/{id}/menu", rid.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // US6: Publish and depublish a single dish (happy path)
    @Test
    void us6_publishAndDepublishDish() throws Exception {
        // Arrange
        Restaurant r = helper.createRestaurant("US6");
        RestaurantId rid = helper.id(r);
        DishId id = helper.addDraftDish(r, "Soup", new BigDecimal("6.50"), "EUR", DishCategory.APPETIZER, "Tomato");

        // Act
        var result = mockMvc.perform(patch("/api/restaurants/{rid}/dishes/{dishId}/publish", rid.id(), id.id()));

        // Assert
        result.andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/restaurants/{rid}/dishes/{dishId}/depublish", rid.id(), id.id()))
                .andExpect(status().isNoContent());
    }

    // US6: Publishing an already published dish should fail
    @Test
    void us6_publishingAlreadyPublishedShouldFail() throws Exception {
        // Arrange
        Restaurant r = helper.createRestaurant("US6-fail");
        RestaurantId rid = helper.id(r);
        DishId id = helper.addDraftDish(r, "Curry", new BigDecimal("11.00"), "EUR", DishCategory.MAIN_COURSE, "Spicy");
        mockMvc.perform(patch("/api/restaurants/{rid}/dishes/{dishId}/publish", rid.id(), id.id()))
                .andExpect(status().isNoContent());

        // Act + Assert: just verify it fails (no message assertion)
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                mockMvc.perform(patch("/api/restaurants/{rid}/dishes/{dishId}/publish", rid.id(), id.id()))
        );

    }

    // US7: Apply all pending changes (publish all drafts)
    @Test
    void us7_publishAllDrafts() throws Exception {
        // Arrange
        Restaurant r = helper.createRestaurant("US7");
        RestaurantId rid = helper.id(r);
        helper.addDraftDish(r, "DishA", new BigDecimal("8.00"), "EUR", DishCategory.MAIN_COURSE, "a");
        helper.addDraftDish(r, "DishB", new BigDecimal("9.00"), "EUR", DishCategory.MAIN_COURSE, "b");

        // Act
        var result = mockMvc.perform(post("/api/restaurants/{rid}/publish_menu", rid.id()));

        // Assert
        result.andExpect(status().isNoContent());
        mockMvc.perform(get("/api/restaurants/{id}/menu", rid.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // US7: No drafts -> no change
    @Test
    void us7_publishAllDraftsNoopWhenNone() throws Exception {
        // Arrange
        Restaurant r = helper.createRestaurant("US7-noop");
        RestaurantId rid = helper.id(r);

        // Act
        var result = mockMvc.perform(post("/api/restaurants/{rid}/publish_menu", rid.id()));

        // Assert
        result.andExpect(status().isNoContent());
        mockMvc.perform(get("/api/restaurants/{id}/menu", rid.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // US8: Schedule publish in the future and process it
    @Test
    void us8_schedulePublishAndProcess() throws Exception {
        // Arrange
        Restaurant r = helper.createRestaurant("US8");
        RestaurantId rid = helper.id(r);
        helper.addDraftDish(r, "Noodles", new BigDecimal("7.50"), "EUR", DishCategory.MAIN_COURSE, "veg");
        String payload = """
        { "publishAt": "%s" }
        """.formatted(LocalDateTime.now().plusSeconds(1));

        // Act
        var result = mockMvc.perform(
                post("/api/restaurants/{rid}/schedule_publish", rid.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload));

        // Assert
        result.andExpect(status().isNoContent());
        JpaScheduledPublishEntity job = scheduledRepo.findAll().getFirst();
        scheduledProcessor.processJob(job.getId());
        mockMvc.perform(get("/api/restaurants/{id}/menu", rid.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    // US8: Scheduling in the past should fail
    @Test
    void us8_schedulePublishInPastShouldFail() throws Exception {
        // Arrange
        Restaurant r = helper.createRestaurant("US8-fail");
        RestaurantId rid = helper.id(r);
        String payload = """
        { "publishAt": "%s" }
        """.formatted(LocalDateTime.now().minusMinutes(1));

        // Act
        var result = mockMvc.perform(
                post("/api/restaurants/{rid}/schedule_publish", rid.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("publishAt must be in the future")));

    }

    // US9: Toggle availability immediately (not schedulable)
    @Test
    void us9_toggleAvailability() throws Exception {
        // Arrange
        Restaurant r = helper.createRestaurant("US9");
        RestaurantId rid = helper.id(r);
        DishId id = helper.addDraftDish(r, "Fries", new BigDecimal("3.00"), "EUR", DishCategory.APPETIZER, "salt");
        mockMvc.perform(patch("/api/restaurants/{rid}/dishes/{dishId}/publish", rid.id(), id.id()))
                .andExpect(status().isNoContent());

        // Act
        var result = mockMvc.perform(
                patch("/api/restaurants/{rid}/dishes/{dishId}/availability", rid.id(), id.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\": false}"));

        // Assert
        result.andExpect(status().isNoContent());
        mockMvc.perform(get("/api/restaurants/{id}/dishes", rid.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Fries')].status", hasItem("OUT_OF_STOCK")));
    }

    // US9: Toggle availability for unknown dish should fail
    @Test
    void us9_toggleAvailabilityUnknownDishShouldFail() throws Exception {
        // Arrange
        Restaurant r = helper.createRestaurant("US9-fail");
        RestaurantId rid = helper.id(r);
        DishId unknown = helper.randomDishId();

        // Act
        var result = mockMvc.perform(
                patch("/api/restaurants/{rid}/dishes/{dishId}/availability", rid.id(), unknown.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\": false}"));

        // Assert
        result.andExpect(status().isNotFound());
    }
}
