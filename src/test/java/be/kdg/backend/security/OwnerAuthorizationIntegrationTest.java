package be.kdg.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OwnerAuthorizationIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    private RequestPostProcessor ownerJwt(String sub) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject(sub).claim("realm_access",
                        java.util.Map.of("roles", java.util.List.of("owner"))))
                .authorities(jwt -> java.util.Collections.emptyList());
    }

    private RequestPostProcessor otherJwt(String sub) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject(sub).claim("realm_access",
                        java.util.Map.of("roles", java.util.List.of("owner"))))
                .authorities(jwt -> java.util.Collections.emptyList());
    }

    @Test
    void public_gets_remain_open() throws Exception {
        mockMvc.perform(get("/api/restaurants"))
                .andExpect(status().isOk());
    }

    @Test
    void owner_create_restaurant_returns_created_with_location() throws Exception {
        // arrange
        String ownerSub = UUID.randomUUID().toString();
        String body = """
        {
          "name":"OwnedResto",
          "fullAddress":"Main Street 1, 1000 Brussels, BE",
          "email":"owner@example.com",
          "openingHours":"Mon-Sun 10:00-22:00",
          "logo":"https://example.com/logo.png"
        }
        """;

        // act
        var result = mockMvc.perform(
                post("/api/restaurants")
                        .with(ownerJwt(ownerSub))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        );

        // assert
        result.andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/restaurants/")));
    }

    @Test
    void owner_can_open_owned_restaurant() throws Exception {
        // arrange
        String ownerSub = UUID.randomUUID().toString();
        String id = createRestaurantAs(ownerSub, "OwnedResto");

        // act
        var result = mockMvc.perform(
                patch("/api/restaurants/{id}/open", id).with(ownerJwt(ownerSub))
        );

        // assert
        result.andExpect(status().isNoContent());
    }

    @Test
    void non_owner_cannot_close_others_restaurant() throws Exception {
        // arrange
        String ownerSub = UUID.randomUUID().toString();
        String otherSub = UUID.randomUUID().toString();
        String id = createRestaurantAs(ownerSub, "OwnedResto");

        // act
        var result = mockMvc.perform(
                patch("/api/restaurants/{id}/close", id).with(otherJwt(otherSub))
        );

        // assert
        result.andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_open_returns_401() throws Exception {
        // arrange
        String ownerSub = UUID.randomUUID().toString();
        String id = createRestaurantAs(ownerSub, "OwnedResto");

        // act
        var result = mockMvc.perform(
                patch("/api/restaurants/{id}/open", id)
        );

        // assert
        result.andExpect(status().isUnauthorized());
    }

    @Test
    void owner_can_create_draft_dish_returns_created() throws Exception {
        // arrange
        String ownerSub = UUID.randomUUID().toString();
        String rid = createRestaurantAs(ownerSub, "DishResto");
        String payload = """
        { "name":"Soup","description":"Tomato","price":{"amount":5.50,"currency":"EUR"}, "category":"APPETIZER" }
        """;

        // act
        var result = mockMvc.perform(
                post("/api/restaurants/{id}/dishes", rid)
                        .with(ownerJwt(ownerSub))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
        );

        // assert
        result.andExpect(status().isCreated());
    }

    @Test
    void non_owner_cannot_publish_dish() throws Exception {
        // arrange
        String ownerSub = UUID.randomUUID().toString();
        String otherSub = UUID.randomUUID().toString();
        String rid = createRestaurantAs(ownerSub, "DishResto");
        String dishId = createDishAs(ownerSub, rid, """
        { "name":"Soup","description":"Tomato","price":{"amount":5.50,"currency":"EUR"}, "category":"APPETIZER" }
        """);

        // act
        var result = mockMvc.perform(
                patch("/api/restaurants/{rid}/dishes/{dishId}/status", rid, dishId)
                        .with(otherJwt(otherSub))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PUBLISHED\"}")
        );

        // assert
        result.andExpect(status().isForbidden());
    }

    // US10: cannot publish more than 10 dishes
    @Test
    void owner_cannot_publish_more_than_10_dishes_returns_conflict() throws Exception {
        // arrange
        String ownerSub = UUID.randomUUID().toString();
        String rid = createRestaurantAs(ownerSub, "CapResto");

        // create 11 draft dishes
        String[] ids = new String[11];
        for (int i = 0; i < 11; i++) {
            String payload = """
            { "name":"Dish-%s","description":"Desc","price":{"amount":10.00,"currency":"EUR"}, "category":"MAIN_COURSE" }
            """.formatted(i + 1);
            ids[i] = createDishAs(ownerSub, rid, payload);
        }

        // publish first 10 -> 204
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(
                    patch("/api/restaurants/{rid}/dishes/{dishId}/status", rid, ids[i])
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"PUBLISHED\"}")
                            .with(ownerJwt(ownerSub))
            ).andExpect(status().isNoContent());
        }

        // attempt to publish 11th -> 409 Conflict
        mockMvc.perform(
                patch("/api/restaurants/{rid}/dishes/{dishId}/status", rid, ids[10])
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"PUBLISHED\"}")
                        .with(ownerJwt(ownerSub))
        ).andExpect(status().isConflict());
    }

    // helpers
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

    // US1: same owner cannot create a second restaurant
    @Test
    void owner_cannot_create_second_restaurant_returns_conflict() throws Exception {
        String ownerSub = UUID.randomUUID().toString();

        String body1 = """
        {
          "name":"R1",
          "fullAddress":"Addr 1",
          "email":"%s@example.com",
          "openingHours":"Mon-Fri 10-18",
          "logo":"https://example.com/logo1.png"
        }
        """.formatted(ownerSub.substring(0, 8));

        mockMvc.perform(
                        post("/api/restaurants")
                                .with(ownerJwt(ownerSub))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body1)
                ).andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/restaurants/")));

        String body2 = """
        {
          "name":"R2",
          "fullAddress":"Addr 2",
          "email":"%s@example.com",
          "openingHours":"Mon-Fri 10-18",
          "logo":"https://example.com/logo2.png"
        }
        """.formatted(ownerSub.substring(0, 8));

        mockMvc.perform(
                post("/api/restaurants")
                        .with(ownerJwt(ownerSub))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body2)
        ).andExpect(status().isConflict());
    }
}
