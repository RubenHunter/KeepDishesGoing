package be.kdg.backend.infrastructure.http;

import be.kdg.backend.application.RestaurantProperties;
import be.kdg.backend.application.restaurant.RestaurantGateway;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestRestaurantGatewayTest {

    private MockWebServer server;
    private RestRestaurantGateway gateway;

    private final UUID restaurantId = UUID.randomUUID();
    private final UUID menuItemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        server = new MockWebServer();
        RestClient client = RestClient.builder().baseUrl(server.url("/").toString()).build();
        RestaurantProperties props = new RestaurantProperties(server.url("/").toString(), "/api/restaurants");
        gateway = new RestRestaurantGateway(client, props, 0.01);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    private String menuJson(String status, double amount) {
        return "[{\"id\":\"" + menuItemId + "\",\"name\":\"Pizza\",\"price\":{\"amount\":" + amount
                + ",\"currency\":\"EUR\"},\"status\":\"" + status + "\",\"category\":\"MAIN_COURSE\",\"description\":\"d\"}]";
    }

    private RestaurantGateway.MenuValidationRequest request(double expectedPrice) {
        return new RestaurantGateway.MenuValidationRequest(
                restaurantId, List.of(new RestaurantGateway.MenuValidationRequest.ItemToValidate(menuItemId, expectedPrice)));
    }

    @Test
    void validateMenuItemsReturnsValidWhenPriceMatchesPublished() {
        server.enqueue(new MockResponse().setBody(menuJson("PUBLISHED", 10.0)).addHeader("Content-Type", "application/json"));

        RestaurantGateway.MenuValidationResult result = gateway.validateMenuItems(request(10.0));

        assertThat(result.valid()).isTrue();
    }

    @Test
    void validateMenuItemsRejectsMissingItem() {
        UUID otherDish = UUID.randomUUID();
        server.enqueue(new MockResponse().setBody("[{\"id\":\"" + otherDish
                + "\",\"name\":\"X\",\"price\":{\"amount\":10.0,\"currency\":\"EUR\"},\"status\":\"PUBLISHED\",\"category\":\"MAIN_COURSE\",\"description\":\"d\"}]")
                .addHeader("Content-Type", "application/json"));

        RestaurantGateway.MenuValidationResult result = gateway.validateMenuItems(request(10.0));

        assertThat(result.valid()).isFalse();
    }

    @Test
    void validateMenuItemsRejectsNotPublished() {
        server.enqueue(new MockResponse().setBody(menuJson("DRAFT", 10.0)).addHeader("Content-Type", "application/json"));

        RestaurantGateway.MenuValidationResult result = gateway.validateMenuItems(request(10.0));

        assertThat(result.valid()).isFalse();
    }

    @Test
    void validateMenuItemsRejectsPriceMismatchBeyondTolerance() {
        server.enqueue(new MockResponse().setBody(menuJson("PUBLISHED", 10.0)).addHeader("Content-Type", "application/json"));

        RestaurantGateway.MenuValidationResult result = gateway.validateMenuItems(request(12.50));

        assertThat(result.valid()).isFalse();
    }

    @Test
    void validateMenuItemsReturnsInvalidOnHttpError() {
        server.enqueue(new MockResponse().setResponseCode(500));

        RestaurantGateway.MenuValidationResult result = gateway.validateMenuItems(request(10.0));

        assertThat(result.valid()).isFalse();
        assertThat(result.message()).isEqualTo("Restaurant menu unavailable");
    }

    @Test
    void validateMenuItemsReturnsInvalidWhenUnreachable() throws Exception {
        server.shutdown();

        RestaurantGateway.MenuValidationResult result = gateway.validateMenuItems(request(10.0));

        assertThat(result.valid()).isFalse();
        assertThat(result.message()).isEqualTo("Restaurant service unreachable");
    }

    @Test
    void getRestaurantMapsActiveToOpen() {
        server.enqueue(new MockResponse().setBody("{\"id\":\"" + restaurantId
                + "\",\"name\":\"Resto\",\"status\":\"ACTIVE\",\"fullAddress\":\"a\",\"email\":\"e\","
                + "\"openingHours\":\"Mon-Sun 11:00-23:00\",\"logoUrl\":\"l\",\"restaurantType\":\"FAST_FOOD\"}")
                .addHeader("Content-Type", "application/json"));

        RestaurantGateway.RestaurantDto dto = gateway.getRestaurant(restaurantId);

        assertThat(dto.open()).isTrue();
    }

    @Test
    void getRestaurantMapsInactiveToClosed() {
        server.enqueue(new MockResponse().setBody("{\"id\":\"" + restaurantId
                + "\",\"name\":\"Resto\",\"status\":\"INACTIVE\",\"fullAddress\":\"a\",\"email\":\"e\","
                + "\"openingHours\":\"Mon-Sun 11:00-23:00\",\"logoUrl\":\"l\",\"restaurantType\":\"FAST_FOOD\"}")
                .addHeader("Content-Type", "application/json"));

        RestaurantGateway.RestaurantDto dto = gateway.getRestaurant(restaurantId);

        assertThat(dto.open()).isFalse();
    }

    @Test
    void getMenuReturnsEmptyWhenBodyNull() {
        server.enqueue(new MockResponse().addHeader("Content-Type", "application/json"));

        List<RestaurantGateway.DishDto> menu = gateway.getMenu(restaurantId);

        assertThat(menu).isEmpty();
    }

    @Test
    void getStatusMapsOpenNow() {
        server.enqueue(new MockResponse().setBody("{\"openNow\":true,\"closingTime\":null,\"nextOpening\":null}")
                .addHeader("Content-Type", "application/json"));

        RestaurantGateway.RestaurantStatusDto status = gateway.getStatus(restaurantId);

        assertThat(status.openNow()).isTrue();
    }

    @Test
    void getRestaurantThrowsOnEmptyResponse() {
        server.enqueue(new MockResponse().addHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> gateway.getRestaurant(restaurantId))
                .isInstanceOf(IllegalStateException.class);
    }
}
