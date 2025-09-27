package be.kdg.backend.api;

import be.kdg.backend.api.dto.*;
import be.kdg.backend.application.DishService;
import be.kdg.backend.domain.dish.Dish;
import be.kdg.backend.domain.dish.DishId;
import be.kdg.backend.domain.restaurant.RestaurantId;
import be.kdg.backend.infrastructure.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@Slf4j
public class DishController {

    private final DishService dishService;

    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    /*
    ? have to rethink these to fit new domain model
     /restaurants/{id}/dishes
     post, owner, req: CreateDishDto,
     get, owner, resp: List<DishDto>
    */
    @PostMapping("/restaurants/{id}/dishes")
    public ResponseEntity<DishDto> createDish(@Valid @RequestBody DishDto dto, @PathVariable final UUID id) {
        log.info("Creating a new dish {}", dto);

        //TODO: still need to connect it to a restaurant
        final RestaurantId restaurantId = new RestaurantId(id);
        Dish created = dishService.createDish(dto.to(), restaurantId);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/" + created.getId()).build().toUri())
            .body(DishDto.from(created));
    }

    @GetMapping("/restaurants/{id}/dishes")
    public ResponseEntity<Iterable<DishDto>> getAllDishes(@RequestParam(defaultValue = "") String name, @PathVariable final UUID id) {
        log.info("REST request to get all Dishes from restaurant with id {}", id);
        final RestaurantId restaurantId = new RestaurantId(id);
        return ResponseEntity.ok(dishService.getAllDishesFromRestaurantById(restaurantId)
            .stream()
            .filter(dish -> dish.getName().name().toLowerCase().contains(name))
            .map(DishDto::from)
            .toList());
    }

    /*
     /restaurants/{id}/menu
     get, public, resp: List<DishDto>
    */
    @GetMapping("/restaurants/{id}/menu")
    public ResponseEntity<Iterable<DishDto>> getMenu(@RequestParam(defaultValue = "") String name, @PathVariable final UUID id) {
        log.info("REST request to get menu from restaurant with id {}", id);
        final RestaurantId restaurantId = new RestaurantId(id);
        return ResponseEntity.ok(dishService.getMenuDishes(restaurantId)
            .stream()
            .filter(dish -> dish.getName().name().toLowerCase().contains(name))
            .map(DishDto::from)
            .toList());
    }

    /*
     /dishes/{dishId}
     put, owner, req: UpdateDishDto, resp: DishDto
    */
    @PutMapping("/restaurants/{restaurantId}/dishes/{dishId}")
    public ResponseEntity<DishDto> updateDish(@Valid @RequestBody UpdateDishDto dto, @PathVariable final UUID restaurantId, @PathVariable final UUID dishId) {
        log.info("Updating dish with id {} from restaurant with id {}: {}", dishId, restaurantId, dto);
        DishDto updated = dishService.updateDish(new RestaurantId(restaurantId), new DishId(dishId), dto);
        return ResponseEntity.ok(updated);

    }

    /*
     /dishes/{dishId}/publish
     patch, owner
    */
    @PatchMapping("/restaurants/{restaurantId}/dishes/{dishId}/publish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void publishDish(@PathVariable final UUID restaurantId,@PathVariable final UUID dishId) {
        log.info("Publishing dish with id {} from restaurant with id {}", dishId, restaurantId);
        try {
            dishService.publishDish(new RestaurantId(restaurantId),new DishId(dishId));
        } catch (EntityNotFoundException e) {
            throw new ResourceNotFoundException("No dish found with id " + dishId, e);
        }
    }

    /*
    /dishes/{dishId}/availability
    patch, owner, req: SetAvailabilityDto
     */
    @PatchMapping("/restaurants/{restaurantId}/dishes/{dishId}/availability")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setDishAvailability(
            @PathVariable final UUID restaurantId,
            @PathVariable final UUID dishId,
            @RequestBody SetAvailabilityDto dto) {
        log.info("Setting availability of dish with id {} from restaurant with id {} to {}", dishId, restaurantId, dto.available());
        try {
            dishService.setDishAvailability(new RestaurantId(restaurantId), new DishId(dishId), dto.available());
        } catch (EntityNotFoundException e) {
            throw new ResourceNotFoundException("No dish found with id " + dishId, e);
        }
    }


    /*
    /restaurants/{id}/publish_menu
    post, owner
     */
    @PostMapping("/restaurants/{restaurantId}/publish_menu")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void publishMenu(@PathVariable final UUID restaurantId) {
        log.info("Publishing all draft dishes for restaurant {}", restaurantId);
        dishService.publishAllDraftDishes(new RestaurantId(restaurantId));
    }



    /*
    /restaurants/{id}/schedule_publish
    post, owner, req: SchedulePublishDto
     */
// src/main/java/be/kdg/backend/api/DishController.java
    @PostMapping("/restaurants/{restaurantId}/schedule_publish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void schedulePublish(
            @PathVariable final UUID restaurantId,
            @RequestBody SchedulePublishDto dto) {
        log.info("Scheduling publish of all draft dishes for restaurant {} at {}", restaurantId, dto.publishAt());
        dishService.schedulePublishAllDraftDishes(new RestaurantId(restaurantId), dto.publishAt());
    }

}
