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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DishControllerIntegrationTest {

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

    @Test
    void shouldCreateAndListDishesAndShowInMenuWhenPublished() throws Exception {
        Restaurant r = helper.createRestaurant("API Resto");
        RestaurantId rid = helper.id(r);

        mockMvc.perform(
                        post("/api/restaurants/{id}/dishes", rid.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "name": "Lasagna",
                                  "description": "Layered pasta with meat and cheese",
                                  "price": { "amount": 14.5, "currency": "EUR" },
                                  "category": "MAIN_COURSE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/restaurants/" + rid.id() + "/dishes/")));

        mockMvc.perform(get("/api/restaurants/{id}/dishes", rid.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Lasagna")))
                .andExpect(jsonPath("$[*].status", everyItem(is(oneOf("DRAFT","PUBLISHED","OUT_OF_STOCK")))));

        // Publish the only dish
        Restaurant reloaded = helper.reload(rid);
        DishId dishId = reloaded.getDishes().getFirst().getId();

        mockMvc.perform(patch("/api/restaurants/{rid}/dishes/{dishId}/publish", rid.id(), dishId.id()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/restaurants/{id}/menu", rid.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Lasagna"))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));
    }

    @Test
    void shouldVersionDishOnUpdateAndReplaceOnPublish() throws Exception {
        Restaurant r = helper.createRestaurant("Version Resto");
        RestaurantId rid = helper.id(r);
        DishId originalId = helper.addDraftDish(r, "Burger", new BigDecimal("10.00"), "EUR", DishCategory.MAIN_COURSE, "Beef burger");
        helper.publishDish(r, originalId);

        // Update published dish -> creates or reuses draft, menu keeps old
        mockMvc.perform(
                        put("/api/restaurants/{rid}/dishes/{dishId}", rid.id(), originalId.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "name": "Burger",
                                  "description": "Beef burger with cheese",
                                  "price": { "amount": 12.5, "currency": "EUR" },
                                  "category": "MAIN_COURSE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Burger"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.price.amount").value(12.5));

        // Menu still shows old version
        mockMvc.perform(get("/api/restaurants/{id}/menu", rid.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].price.amount").value(10.0));

        // Publish draft -> replaces menu item
        Restaurant afterUpdate = helper.reload(rid);
        Dish draft = afterUpdate.getDishes().stream().filter(d -> d.getStatus() == DishStatus.DRAFT && d.getName().name().equals("Burger")).findFirst().orElseThrow();
        mockMvc.perform(patch("/api/restaurants/{rid}/dishes/{dishId}/publish", rid.id(), draft.getId().id()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/restaurants/{id}/menu", rid.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].price.amount").value(12.5))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void shouldDepublishAndToggleAvailability() throws Exception {
        Restaurant r = helper.createRestaurant("Avail Resto");
        RestaurantId rid = helper.id(r);
        DishId id = helper.addDraftDish(r, "Soup", new BigDecimal("6.00"), "EUR", DishCategory.APPETIZER, "Tomato soup");
        helper.publishDish(r, id);

        // Out of stock
        mockMvc.perform(
                        patch("/api/restaurants/{rid}/dishes/{dishId}/availability", rid.id(), id.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"available\": false}"))
                .andExpect(status().isNoContent());

        // Back available -> publish
        mockMvc.perform(
                        patch("/api/restaurants/{rid}/dishes/{dishId}/availability", rid.id(), id.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"available\": true}"))
                .andExpect(status().isNoContent());

        // Depublish
        mockMvc.perform(patch("/api/restaurants/{rid}/dishes/{dishId}/depublish", rid.id(), id.id()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldPublishAllDrafts() throws Exception {
        Restaurant r = helper.createRestaurant("Batch Resto");
        RestaurantId rid = helper.id(r);
        helper.addDraftDish(r, "Pasta", new BigDecimal("9.50"), "EUR", DishCategory.MAIN_COURSE, "Pesto");
        helper.addDraftDish(r, "Salad", new BigDecimal("7.00"), "EUR", DishCategory.APPETIZER, "Caesar");

        mockMvc.perform(post("/api/restaurants/{rid}/publish_menu", rid.id()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/restaurants/{id}/menu", rid.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void shouldScheduleAndProcessPublishJob() throws Exception {
        Restaurant r = helper.createRestaurant("Sched Resto");
        RestaurantId rid = helper.id(r);
        helper.addDraftDish(r, "Curry", new BigDecimal("11.00"), "EUR", DishCategory.MAIN_COURSE, "Spicy");

        mockMvc.perform(
                        post("/api/restaurants/{rid}/schedule_publish", rid.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                 { "publishAt": "%s" }
                                 """.formatted(LocalDateTime.now().plusSeconds(1))))
                .andExpect(status().isNoContent());

        // Pick job and process explicitly (no need to wait for scheduler)
        JpaScheduledPublishEntity job = scheduledRepo.findAll().getFirst();
        scheduledProcessor.processJob(job.getId());

        // Verify menu published
        mockMvc.perform(get("/api/restaurants/{id}/menu", rid.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
