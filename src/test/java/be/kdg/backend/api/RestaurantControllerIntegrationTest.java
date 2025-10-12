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

    @Test
    void shouldCreateRestaurant() throws Exception {
        // Arrange

        // Act
        mockMvc.perform(
                        post("/api/restaurants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Test Restaurant\"}"))
                // Assert
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/restaurants/")));
    }

    @Test
    void shouldListRestaurants() throws Exception {
        // Arrange
        helper.createRestaurant("A");
        helper.createRestaurant("B");

        // Act
        mockMvc.perform(get("/api/restaurants"))
                // Assert
                .andExpect(status().isOk());
        // Optionally check the number of restaurants returned
        //.andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void shouldOpenRestaurant() throws Exception {
        // Arrange
        Restaurant r = helper.createRestaurant("Toggle R");
        RestaurantId rid = helper.id(r);

        // Act
        mockMvc.perform(patch("/api/restaurants/{id}/open", rid.id()))
                // Assert
                .andExpect(status().isNoContent());

        // Act
        mockMvc.perform(get("/api/restaurants/{id}", rid.id()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldCloseRestaurant() throws Exception {
        // Arrange
        Restaurant r = helper.createRestaurant("Toggle R");
        RestaurantId rid = helper.id(r);

        // Act
        //First open the restaurant so we can close it
        mockMvc.perform(patch("/api/restaurants/{id}/open", rid.id()))
                // Assert
                .andExpect(status().isNoContent());

        // Act
        mockMvc.perform(patch("/api/restaurants/{id}/close", rid.id()))
                // Assert
                .andExpect(status().isNoContent());

        // Act
        mockMvc.perform(get("/api/restaurants/{id}", rid.id()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

}
