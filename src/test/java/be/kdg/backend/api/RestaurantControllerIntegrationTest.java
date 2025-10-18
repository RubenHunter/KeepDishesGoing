package be.kdg.backend.api;

import be.kdg.backend.TestHelper;
import be.kdg.backend.domain.restaurant.Restaurant;
import be.kdg.backend.domain.restaurant.RestaurantId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RestaurantControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestHelper helper;

    private RequestPostProcessor ownerJwt(String sub) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject(sub).claim("realm_access",
                        java.util.Map.of("roles", java.util.List.of("owner"))));
    }

    @AfterEach
    void tearDown() {
        helper.cleanUp();
    }

    // US2: Create restaurant
    @Test
    void us2_shouldCreateRestaurant() throws Exception {
        // Arrange
        String sub = java.util.UUID.randomUUID().toString();
        String body = "{\"name\":\"Story Restaurant\"}";

        // Act
        var result = mockMvc.perform(
                post("/api/restaurants")
                        .with(ownerJwt(sub))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));

        // Assert
        result.andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/restaurants/")));
    }

    // US12: Open/Close manually (happy path)
    @Test
    void us12_shouldOpenRestaurant() throws Exception {
        // Arrange
        String sub = java.util.UUID.randomUUID().toString();
        var r = helper.createRestaurant("US12-open");
        var rid = helper.id(r);

        // Act
        mockMvc.perform(patch("/api/restaurants/{id}/open", rid.id()).with(ownerJwt(sub)))
                .andExpect(status().isNoContent());

        // Assert: verify state changed
        mockMvc.perform(get("/api/restaurants/{id}", rid.id()).with(ownerJwt(sub)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // US12: Opening an already open restaurant should fail
    @Test
    void us12_openingAlreadyOpenShouldFail() throws Exception {
        // Arrange
        String sub = java.util.UUID.randomUUID().toString();
        Restaurant r = helper.createRestaurant("Open twice");
        RestaurantId rid = helper.id(r);
        mockMvc.perform(patch("/api/restaurants/{id}/open", rid.id()).with(ownerJwt(sub))).andExpect(status().isNoContent());

        // Act & Assert
        mockMvc.perform(patch("/api/restaurants/{id}/open", rid.id()).with(ownerJwt(sub)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message", containsString("already open")));
    }

}
