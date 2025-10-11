package be.kdg.backend.api;

import be.kdg.backend.api.dto.CreateRestaurantDto;
import be.kdg.backend.api.dto.RestaurantDto;
import be.kdg.backend.application.RestaurantService;
import be.kdg.backend.domain.restaurant.Restaurant;
import be.kdg.backend.domain.restaurant.RestaurantId;
import be.kdg.backend.domain.restaurant.RestaurantStatus;
import be.kdg.backend.infrastructure.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    /*
    /restaurants
    post, auth -> owner, req:CreateRestaurantDto resp: RestaurantDto
    get, public, resp: List<RestaurantDto>
    */
    @PostMapping({"", "/"})
    public ResponseEntity<Void> createRestaurant(@Valid @RequestBody CreateRestaurantDto dto) {
        RestaurantId createdId = restaurantService.createRestaurant(dto.name());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdId.id())
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @GetMapping({"", "/"})
    public ResponseEntity<Iterable<RestaurantDto>> getAllRestaurants(/*@RequestParam(defaultValue = "") String name*/) {
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
    public ResponseEntity<Void> closeRestaurant(@PathVariable final UUID id) {
        final RestaurantId restaurantId = new RestaurantId(id);
        restaurantService.closeRestaurant(restaurantId);
        return ResponseEntity.noContent().build();

    }

}
