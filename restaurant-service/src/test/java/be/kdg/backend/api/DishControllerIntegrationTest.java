package be.kdg.backend.api;

import be.kdg.backend.TestHelper;
import be.kdg.backend.application.ScheduledPublishJobRunner;
import be.kdg.backend.infrastructure.jpa.JpaScheduledPublishEntity;
import be.kdg.backend.infrastructure.jpa.JpaScheduledPublishRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private ScheduledPublishJobRunner jobRunner;

    private RequestPostProcessor ownerJwt(String sub) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject(sub).claim("realm_access",
                        java.util.Map.of("roles", java.util.List.of("owner"))))
                .authorities(jwt -> java.util.Collections.emptyList());
    }

    @AfterEach
    void tearDown() {
        helper.cleanUp();
    }

    // US4: Draft should not affect live menu
    @Test
    void us4_draftDoesNotAffectMenu() throws Exception {
        String sub = java.util.UUID.randomUUID().toString();
        String rid = createRestaurantAs(sub, "US4");

        String payload = """
        {
          "name": "Tagliatelle",
          "description": "Mushroom sauce",
          "price": { "amount": 13.0, "currency": "EUR" },
          "category": "MAIN_COURSE"
        }
        """;

        mockMvc.perform(
                        post("/api/restaurants/{id}/dishes", rid)
                                .with(ownerJwt(sub))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/restaurants/{id}/menu", rid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // US6: Publish and depublish a single dish (happy path)
    @Test
    void us6_publishAndDepublishDish() throws Exception {
        String sub = java.util.UUID.randomUUID().toString();
        String rid = createRestaurantAs(sub, "US6");
        String dishId = createDishAs(sub, rid, """
        { "name":"Soup","description":"Tomato","price":{"amount":6.50,"currency":"EUR"}, "category":"APPETIZER" }
        """);

        mockMvc.perform(patch("/api/restaurants/{rid}/dishes/{dishId}/status", rid, dishId).with(ownerJwt(sub))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/api/restaurants/{rid}/dishes/{dishId}/status", rid, dishId).with(ownerJwt(sub))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DRAFT\"}"))
                .andExpect(status().isNoContent());
    }

    // US6: Publishing an already published dish should fail
    @Test
    void us6_publishingAlreadyPublishedShouldFail() throws Exception {
        String sub = java.util.UUID.randomUUID().toString();
        String rid = createRestaurantAs(sub, "US6-fail");
        String dishId = createDishAs(sub, rid, """
        { "name":"Curry","description":"Spicy","price":{"amount":11.00,"currency":"EUR"}, "category":"MAIN_COURSE" }
        """);

        mockMvc.perform(patch("/api/restaurants/{rid}/dishes/{dishId}/status", rid, dishId).with(ownerJwt(sub))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/api/restaurants/{rid}/dishes/{dishId}/status", rid, dishId).with(ownerJwt(sub))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message", containsString("already published")));
    }

    // US7: Apply all pending changes (publish all drafts)
    @Test
    void us7_publishAllDrafts() throws Exception {
        String sub = java.util.UUID.randomUUID().toString();
        String rid = createRestaurantAs(sub, "US7");

        createDishAs(sub, rid, """
        { "name":"DishA","description":"a","price":{"amount":8.00,"currency":"EUR"}, "category":"MAIN_COURSE" }
        """);
        createDishAs(sub, rid, """
        { "name":"DishB","description":"b","price":{"amount":9.00,"currency":"EUR"}, "category":"MAIN_COURSE" }
        """);

        mockMvc.perform(post("/api/restaurants/{rid}/menu/publications", rid).with(ownerJwt(sub)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/restaurants/{id}/menu", rid).with(ownerJwt(sub)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // US7: No drafts -> no change
    @Test
    void us7_publishAllDraftsNoopWhenNone() throws Exception {
        String sub = java.util.UUID.randomUUID().toString();
        String rid = createRestaurantAs(sub, "US7-noop");

        mockMvc.perform(post("/api/restaurants/{rid}/menu/publications", rid).with(ownerJwt(sub)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/restaurants/{id}/menu", rid).with(ownerJwt(sub)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // US8: Schedule publish in the future and process it
    @Test
    void us8_schedulePublishAndProcess() throws Exception {
        String sub = java.util.UUID.randomUUID().toString();
        String rid = createRestaurantAs(sub, "US8");
        createDishAs(sub, rid, """
        { "name":"Noodles","description":"veg","price":{"amount":7.50,"currency":"EUR"}, "category":"MAIN_COURSE" }
        """);

        String payload = """
        { "publishAt": "%s" }
        """.formatted(LocalDateTime.now().plusSeconds(1));

        mockMvc.perform(
                        post("/api/restaurants/{rid}/menu/publications", rid)
                                .with(ownerJwt(sub))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                .andExpect(status().isAccepted());

        JpaScheduledPublishEntity job = scheduledRepo.findAll().getFirst();
        jobRunner.run(job.getId());

        mockMvc.perform(get("/api/restaurants/{id}/menu", rid).with(ownerJwt(sub)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    // US8: Scheduling in the past should fail
    @Test
    void us8_schedulePublishInPastShouldFail() throws Exception {
        String sub = java.util.UUID.randomUUID().toString();
        String rid = createRestaurantAs(sub, "US8-fail");

        String payload = """
        { "publishAt": "%s" }
        """.formatted(LocalDateTime.now().minusMinutes(1));

        mockMvc.perform(
                        post("/api/restaurants/{rid}/menu/publications", rid)
                                .with(ownerJwt(sub))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("publishAt must be in the future")));
    }

    // US9: Toggle availability immediately (not schedulable)
    @Test
    void us9_toggleAvailability() throws Exception {
        String sub = java.util.UUID.randomUUID().toString();
        String rid = createRestaurantAs(sub, "US9");
        String dishId = createDishAs(sub, rid, """
        { "name":"Fries","description":"salt","price":{"amount":3.00,"currency":"EUR"}, "category":"APPETIZER" }
        """);

        mockMvc.perform(patch("/api/restaurants/{rid}/dishes/{dishId}/status", rid, dishId).with(ownerJwt(sub))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        patch("/api/restaurants/{rid}/dishes/{dishId}/status", rid, dishId)
                                .with(ownerJwt(sub))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"OUT_OF_STOCK\",\"available\": false}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/restaurants/{id}/dishes", rid).with(ownerJwt(sub)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Fries')].status", hasItem("OUT_OF_STOCK")));
    }

    // US9: Toggle availability for unknown dish should fail
    @Test
    void us9_toggleAvailabilityUnknownDishShouldFail() throws Exception {
        String sub = java.util.UUID.randomUUID().toString();
        String rid = createRestaurantAs(sub, "US9-fail");
        String unknownDishId = UUID.randomUUID().toString();

        mockMvc.perform(
                        patch("/api/restaurants/{rid}/dishes/{dishId}/status", rid, unknownDishId)
                                .with(ownerJwt(sub))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"OUT_OF_STOCK\",\"available\": false}"))
                .andExpect(status().isNotFound());
    }

    // helpers (copied from OwnerAuthorizationIntegrationTest)
    private String createRestaurantAs(String sub, String name) throws Exception {
        String body = """
        {
          "name":"%s",
          "fullAddress":"Main Street 1, 1000 Brussels, BE",
          "email":"%s@example.com",
          "openingHours":"Mon-Sun 10:00-22:00",
          "logo":"https://example.com/logo.png"
        }
        """.formatted(name, sub.substring(0, 8));

        var resp = mockMvc.perform(
                        post("/api/restaurants")
                                .with(ownerJwt(sub))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                ).andExpect(status().isCreated())
                .andReturn();

        String location = resp.getResponse().getHeader("Location");
        if (location == null) throw new IllegalStateException("Location header missing");
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private String createDishAs(String sub, String rid, String payload) throws Exception {
        var resp = mockMvc.perform(
                        post("/api/restaurants/{id}/dishes", rid)
                                .with(ownerJwt(sub))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                ).andExpect(status().isCreated())
                .andReturn();

        String location = resp.getResponse().getHeader("Location");
        if (location == null) throw new IllegalStateException("Location header missing");
        return location.substring(location.lastIndexOf('/') + 1);
    }
}
