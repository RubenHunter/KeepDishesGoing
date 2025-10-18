package be.kdg.backend.security;

import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

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
                .authorities(jwt -> java.util.Collections.emptyList());// roles mapped by converter (not used by MockMvc)
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
        String body = "{\"name\":\"OwnedResto\"}";

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
                patch("/api/restaurants/{rid}/dishes/{dishId}/publish", rid, dishId)
                        .with(otherJwt(otherSub))
        );

        // assert
        result.andExpect(status().isForbidden());
    }

    // helpers
    private String createRestaurantAs(String sub, String name) throws Exception {
        var resp = mockMvc.perform(
                post("/api/restaurants")
                        .with(ownerJwt(sub))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}")
        ).andReturn();
        String location = resp.getResponse().getHeader("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private String createDishAs(String sub, String rid, String payload) throws Exception {
        var resp = mockMvc.perform(
                post("/api/restaurants/{id}/dishes", rid)
                        .with(ownerJwt(sub))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
        ).andReturn();
        String location = resp.getResponse().getHeader("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }
}
