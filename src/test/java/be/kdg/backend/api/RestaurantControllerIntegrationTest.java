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
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RestaurantControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestHelper helper;

    @AfterEach
    void tearDown() {
        helper.cleanUp();
    }

    // US2: Create restaurant
    @Test
    void us2_shouldCreateRestaurant() throws Exception {
        // Arrange
        String body = "{\"name\":\"Story Restaurant\"}";

        // Act
        var result = mockMvc.perform(
                post("/api/restaurants")
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
        Restaurant r = helper.createRestaurant("Openable");
        RestaurantId rid = helper.id(r);

        // Act
        var result = mockMvc.perform(patch("/api/restaurants/{id}/open", rid.id()));

        // Assert
        result.andExpect(status().isNoContent());
    }

    // US12: Opening an already open restaurant should fail
    @Test
    void us12_openingAlreadyOpenShouldFail() throws Exception {
        // Arrange
        Restaurant r = helper.createRestaurant("Open twice");
        RestaurantId rid = helper.id(r);
        mockMvc.perform(patch("/api/restaurants/{id}/open", rid.id())).andExpect(status().isNoContent());

        // Act & Assert
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                mockMvc.perform(patch("/api/restaurants/{id}/open", rid.id()))
        );


    }

}
