package be.kdg.backend.api;

import be.kdg.backend.api.dto.*;
import be.kdg.backend.application.DishService;
import be.kdg.backend.application.RestaurantService;
import be.kdg.backend.domain.NotFoundException;
import be.kdg.backend.domain.dish.Dish;
import be.kdg.backend.domain.dish.DishId;
import be.kdg.backend.domain.dish.DishStatus;
import be.kdg.backend.domain.restaurant.Restaurant;
import be.kdg.backend.domain.restaurant.RestaurantId;
import be.kdg.backend.domain.restaurant.RestaurantStatus;
import be.kdg.backend.infrastructure.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/restaurants")
@Slf4j //lombok annotation for logging
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final DishService dishService;
    public RestaurantController(RestaurantService restaurantService, DishService dishService) {
        this.restaurantService = restaurantService;
        this.dishService = dishService;
    }

    /*
    /restaurants
    post, auth -> owner, req:CreateRestaurantDto resp: RestaurantDto
    get, public, resp: List<RestaurantDto>
    */
    @PostMapping({"", "/"})
    public ResponseEntity<RestaurantDto> createRestaurant(@Valid @RequestBody CreateRestaurantDto dto, JwtAuthenticationToken jwt) {
        UUID ownerId = UUID.fromString(jwt.getToken().getSubject());
        RestaurantId createdId = restaurantService.createRestaurant(dto.name(), ownerId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdId.id())
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @GetMapping({"", "/"})
    public ResponseEntity<Iterable<RestaurantDto>> getAllRestaurants() {
        log.info("REST request to get all Restaurants");
        List<Restaurant> allRestaurants = restaurantService.listRestaurants();
        List<RestaurantDto> restaurantDtos = allRestaurants.stream()
                .map(RestaurantDto::from)
                .toList();
        return ResponseEntity.ok(restaurantDtos);
    }


    /*
    /restaurants/{id}
    get, public, resp: RestaurantDto
     */
    @GetMapping("/{id}")
    public ResponseEntity<RestaurantDto> getRestaurantById(@PathVariable final UUID id) {
        final RestaurantId restaurantId = new RestaurantId(id);
        final Restaurant restaurant = restaurantService.getRestaurantById(restaurantId);
        return ResponseEntity.ok(RestaurantDto.from(restaurant));
    }


    /*
    /restaurants/{id}/open
    patch, auth -> owner
     */
    @PatchMapping("/{id}/open")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@ownerGuard.canManageRestaurant(#id)")
    public ResponseEntity<Void> openRestaurant(@PathVariable final UUID id) {
        final RestaurantId restaurantId = new RestaurantId(id);
        restaurantService.openRestaurant(restaurantId);
        return ResponseEntity.noContent().build();
    }


    /*
    /restaurants/{id}/close
    patch, auth -> owner
     */
    @PatchMapping("/{id}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@ownerGuard.canManageRestaurant(#id)")
    public ResponseEntity<Void> closeRestaurant(@PathVariable final UUID id) {
        final RestaurantId restaurantId = new RestaurantId(id);
        restaurantService.closeRestaurant(restaurantId);
        return ResponseEntity.noContent().build();

    }

    @GetMapping("/{id}/status")
    public ResponseEntity<RestaurantStatusResponse> getRestaurantStatus(@PathVariable final UUID id) {
        final RestaurantId restaurantId = new RestaurantId(id);
        final Restaurant restaurant = restaurantService.getRestaurantById(restaurantId);

        boolean isOpen = restaurant.getStatus() == RestaurantStatus.ACTIVE; // Vereenvoudigd
        String statusMessage = isOpen ? "Restaurant is open" : "Restaurant is closed";

        return ResponseEntity.ok(new RestaurantStatusResponse(isOpen, restaurant.getStatus().name(), statusMessage));
    }

    @PostMapping("/{restaurantId}/menu/{menuItemId}/validate")
    public ResponseEntity<MenuItemValidationResponse> validateMenuItem(
            @PathVariable final UUID restaurantId,
            @PathVariable final UUID menuItemId,
            @RequestBody @Valid MenuItemValidationRequest request) {

        final RestaurantId restId = new RestaurantId(restaurantId);
        final DishId dishId = new DishId(menuItemId);

        try {
            Dish dish = dishService.getDishById(dishId);
            Restaurant restaurant = restaurantService.getRestaurantById(restId);

            // Valideer beschikbaarheid
            if (dish.getStatus() != DishStatus.PUBLISHED) {
                return ResponseEntity.ok(new MenuItemValidationResponse(
                        false, "Dish is not available", null, null, false
                ));
            }

            // Valideer prijs (binnen tolerantie)
            double currentPrice = dish.getPrice().amount().doubleValue();
            double expectedPrice = request.expectedPrice();
            double priceTolerance = 0.01; // 1 cent tolerantie

            boolean priceValid = Math.abs(currentPrice - expectedPrice) <= priceTolerance;
            String message = priceValid ? "Validation successful" :
                    String.format("Price mismatch. Current: %.2f, Expected: %.2f", currentPrice, expectedPrice);

            return ResponseEntity.ok(new MenuItemValidationResponse(
                    priceValid && dish.getStatus() == DishStatus.PUBLISHED,
                    message,
                    currentPrice,
                    "EUR", // Aanname
                    dish.getStatus() == DishStatus.PUBLISHED
            ));

        } catch (NotFoundException e) {
            return ResponseEntity.ok(new MenuItemValidationResponse(
                    false, "Menu item not found", null, null, false
            ));
        }
    }
}
