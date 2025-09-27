package be.kdg.backend.api;

import be.kdg.backend.api.dto.RestaurantDto;
import be.kdg.backend.application.RestaurantService;
import be.kdg.backend.domain.restaurant.Restaurant;
import be.kdg.backend.infrastructure.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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
    public ResponseEntity<RestaurantDto> createRestaurant(@Valid @RequestBody RestaurantDto dto) {
        log.info("inserting {}",dto);
        Restaurant created = restaurantService.createRestaurant(dto.to());
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/" + created.getId()).build().toUri())
            .body(RestaurantDto.from(created));
    }
    @GetMapping({"", "/"})
    public ResponseEntity<Iterable<RestaurantDto>> getAllRestaurants(@RequestParam(defaultValue = "") String name) {
        log.info("REST request to get all Restaurants");
        return ResponseEntity.ok(restaurantService.getAllRestaurants()
            .stream()
            .filter(restaurant -> restaurant.getName().toLowerCase().contains(name))
            .map(RestaurantDto::from)
            .toList());
    }


    /*
    /restaurants/{id}
    get, public, resp: RestaurantDto
     */
    @GetMapping("/{id}")
    public ResponseEntity<RestaurantDto> getRestaurantById(@PathVariable long id) {
        return restaurantService.getRestaurantById(id)
            .map(RestaurantDto::from)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }


    /*
    /restaurants/{id}/open
    patch, auth -> owner
     */
    // src/main/java/be/kdg/backend/api/RestaurantController.java
    @PatchMapping("/{id}/open")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> openRestaurant(@PathVariable long id) {
        try {
            Restaurant restaurant = restaurantService.getRestaurantById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("No restaurant found with id " + id));
            restaurant.setIsActive(true);
            restaurantService.updateRestaurant(restaurant);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            throw new ResourceNotFoundException("No restaurant found with id " + id, e);
        }
    }



    /*
    /restaurants/{id}/close
    patch, auth -> owner
     */
    @PatchMapping("/{id}close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> closeRestaurant(@PathVariable long id) {
        try {
            Restaurant restaurant = restaurantService.getRestaurantById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("No restaurant found with id " + id));
            restaurant.setIsActive(false);
            restaurantService.updateRestaurant(restaurant);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            throw new ResourceNotFoundException("No restaurant found with id " + id, e);
        }
    }

}
