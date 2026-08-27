package be.kdg.backend.api;

import be.kdg.backend.api.dto.*;
import be.kdg.backend.application.DishService;
import be.kdg.backend.application.MenuItemValidation;
import be.kdg.backend.application.RestaurantService;
import be.kdg.backend.domain.dish.DishId;
import be.kdg.backend.domain.restaurant.Restaurant;
import be.kdg.backend.domain.restaurant.RestaurantId;
import be.kdg.backend.domain.restaurant.RestaurantStatus;
import be.kdg.backend.domain.restaurant.RestaurantType;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
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
        RestaurantType type = (dto.restaurantType() == null || dto.restaurantType().isBlank())
                ? null
                : RestaurantType.valueOf(dto.restaurantType().toUpperCase(Locale.ROOT));
        RestaurantId createdId = restaurantService.createRestaurant(
                dto.name(),
                dto.fullAddress(),
                dto.email(),
                dto.openingHours(),
                dto.logo(),
                ownerId,
                type
        );
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdId.id())
                .toUri();

        return ResponseEntity.created(location).build();
    }

    /*
    /restaurants/mine
    get, auth -> owner, resp: RestaurantDto (404 when the owner has none yet)
     */
    @GetMapping("/mine")
    public ResponseEntity<RestaurantDto> getMyRestaurant(JwtAuthenticationToken jwt) {
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID ownerId = UUID.fromString(jwt.getToken().getSubject());
        return restaurantService.getRestaurantByOwner(ownerId)
                .map(RestaurantDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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

        LocalDateTime now = LocalDateTime.now();
        boolean openNow = restaurant.isOpenOn(now);
        LocalDateTime closingTime = restaurant.closingAt(now).orElse(null);
        LocalDateTime nextOpening = openNow ? null : restaurant.nextOpeningAfter(now).orElse(null);
        boolean isOpen = restaurant.getStatus() == RestaurantStatus.ACTIVE;
        String statusMessage = openNow ? "Restaurant is open" : "Restaurant is closed";

        return ResponseEntity.ok(new RestaurantStatusResponse(
                isOpen, restaurant.getStatus().name(), statusMessage, openNow, closingTime, nextOpening));
    }

    @PostMapping("/{restaurantId}/menu/{menuItemId}/validate")
    public ResponseEntity<MenuItemValidationResponse> validateMenuItem(
            @PathVariable final UUID restaurantId,
            @PathVariable final UUID menuItemId,
            @RequestBody @Valid MenuItemValidationRequest request) {

        MenuItemValidation result = dishService.validateMenuItem(
                new RestaurantId(restaurantId), new DishId(menuItemId), request.expectedPrice());

        return ResponseEntity.ok(new MenuItemValidationResponse(
                result.isValid(), result.message(), result.currentPrice(),
                result.currentCurrency(), result.isAvailable()));
    }
}
