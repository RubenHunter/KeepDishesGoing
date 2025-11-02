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
                        java.util.Map.of("roles", java.util.List.of("owner"))))
                .authorities(jwt -> java.util.Collections.emptyList());
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
        String body = """
        { 
          "name":"Story Restaurant",
          "fullAddress":"Main Street 1, 1000 Brussels, BE",
          "email":"owner@example.com",
          "openingHours":"Mon-Sun 10:00-22:00",
          "logo":"https://example.com/logo.png"
        }
        """;

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
        String id = createRestaurantAs(sub, "US12-open");

        // Act
        mockMvc.perform(patch("/api/restaurants/{id}/open", id).with(ownerJwt(sub)))
                .andExpect(status().isNoContent());

        // Assert: verify state changed
        mockMvc.perform(get("/api/restaurants/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // US12: Opening an already open restaurant should fail
    @Test
    void us12_openingAlreadyOpenShouldFail() throws Exception {
        // Arrange
        String sub = java.util.UUID.randomUUID().toString();
        String id = createRestaurantAs(sub, "Open twice");
        mockMvc.perform(patch("/api/restaurants/{id}/open", id).with(ownerJwt(sub)))
                .andExpect(status().isNoContent());

        // Act & Assert
        mockMvc.perform(patch("/api/restaurants/{id}/open", id).with(ownerJwt(sub)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message", containsString("already open")));
    }

    // helpers (same approach as in OwnerAuthorizationIntegrationTest)
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
}
