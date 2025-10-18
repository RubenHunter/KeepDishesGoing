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

/*
 Requires:
 - Spring Security configured as OAuth2 Resource Server for JWT
 - Controllers protected with @PreAuthorize and role/owner checks
 - Restaurant creation sets ownerId from token's sub
*/
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
    void create_and_open_by_owner_succeeds_but_other_user_forbidden() throws Exception {
        String ownerSub = UUID.randomUUID().toString();
        String otherSub = UUID.randomUUID().toString();

        // Owner creates a restaurant
        var create = mockMvc.perform(
                        post("/api/restaurants")
                                .with(ownerJwt(ownerSub))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"OwnedResto\"}")
                ).andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/restaurants/")))
                .andReturn();

        // Extract created id from Location
        String location = create.getResponse().getHeader("Location");
        String id = location.substring(location.lastIndexOf('/') + 1);

        // Owner can open
        mockMvc.perform(patch("/api/restaurants/{id}/open", id).with(ownerJwt(ownerSub)))
                .andExpect(status().isNoContent());

        // Non-owner forbidden
        mockMvc.perform(patch("/api/restaurants/{id}/close", id).with(otherJwt(otherSub)))
                .andExpect(status().isForbidden());

        // Unauthenticated -> 401
        mockMvc.perform(patch("/api/restaurants/{id}/open", id))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dish_mutations_require_owner_of_that_restaurant() throws Exception {
        String ownerSub = UUID.randomUUID().toString();
        String otherSub = UUID.randomUUID().toString();

        // Create restaurant as owner
        var create = mockMvc.perform(
                post("/api/restaurants").with(ownerJwt(ownerSub))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"DishResto\"}")
        ).andExpect(status().isCreated()).andReturn();

        String rid = create.getResponse().getHeader("Location").replaceAll(".*/", "");

        // Create a draft dish as owner
        String payload = """
        { "name":"Soup","description":"Tomato","price":{"amount":5.50,"currency":"EUR"}, "category":"APPETIZER" }
        """;
        mockMvc.perform(
                post("/api/restaurants/{id}/dishes", rid).with(ownerJwt(ownerSub))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
        ).andExpect(status().isCreated());

        // Other user cannot publish
        mockMvc.perform(
                patch("/api/restaurants/{rid}/dishes/{dishId}/publish", rid, UUID.randomUUID())
                        .with(otherJwt(otherSub))
        ).andExpect(status().isForbidden());
    }
}
