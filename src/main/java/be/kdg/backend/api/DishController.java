package be.kdg.backend.api;

import be.kdg.backend.api.dto.*;
import be.kdg.backend.application.DishService;
import be.kdg.backend.domain.dish.*;
import be.kdg.backend.domain.restaurant.RestaurantId;
import be.kdg.backend.infrastructure.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Locale;
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
    public ResponseEntity<Void> createDish(@Valid @RequestBody DishDto dto, @PathVariable final UUID id) {
        log.info("Creating a new dish {}", dto);
        final RestaurantId restaurantId = new RestaurantId(id);

        DishCategory categoryEnum = (dto.category() == null || dto.category().isBlank())
                ? null
                : DishCategory.valueOf(dto.category().toUpperCase(Locale.ROOT));


        DishId createdId = dishService.createDraftDish(
                restaurantId,
                new DishName(dto.name()),
                new Description(dto.description()),
                dto.price(),
                categoryEnum
        );

        return ResponseEntity.created(
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{dishId}")
                        .buildAndExpand(createdId.id())
                        .toUri()
        ).build();
    }

    // GET /restaurants/{id}/dishes -> service returns DTOs; optional name filter handled in service/DB
    @GetMapping("/restaurants/{id}/dishes")
    public ResponseEntity<List<DishDto>> getAllDishes(
            @PathVariable final UUID id) {
        log.info("REST request to get all Dishes from restaurant with id {}", id);
        final RestaurantId restaurantId = new RestaurantId(id);
        List<Dish> dishes = dishService.listDishesOfRestaurant(restaurantId);
        List<DishDto> dishDtos = dishes.stream()
                .map(DishDto::from)
                .toList();
        return ResponseEntity.ok(dishDtos);
    }

    /*
     /restaurants/{id}/menu
     get, public, resp: List<DishDto>
    */
    @GetMapping("/restaurants/{id}/menu")
    public ResponseEntity<List<DishDto>> getMenu(
            @PathVariable final UUID id) {
        log.info("REST request to get menu from restaurant with id {}", id);
        final RestaurantId restaurantId = new RestaurantId(id);
        List<Dish> dishes = dishService.getMenuDishes(restaurantId);
        List<DishDto> dishDtos = dishes.stream()
                .map(DishDto::from)
                .toList();
        return ResponseEntity.ok(dishDtos);
    }

    /*
     /dishes/{dishId}
     put, owner, req: UpdateDishDto, resp: DishDto
    */
    @PutMapping("/restaurants/{restaurantId}/dishes/{dishId}")
    public ResponseEntity<DishDto> updateDish(@Valid @RequestBody UpdateDishDto dto, @PathVariable final UUID restaurantId, @PathVariable final UUID dishId) {
        log.info("Updating dish with id {} from restaurant with id {}: {}", dishId, restaurantId, dto);
        DishDto updated = dishService.updateDraftDish(new RestaurantId(restaurantId), new DishId(dishId), dto);
        return ResponseEntity.ok(updated);
    }

    /*
     /dishes/{dishId}/publish
     patch, owner
    */
    @PatchMapping("/restaurants/{restaurantId}/dishes/{dishId}/publish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void publishDish(@PathVariable final UUID restaurantId, @PathVariable final UUID dishId) {
        log.info("Publishing dish with id {} from restaurant with id {}", dishId, restaurantId);
        dishService.publishDish(new RestaurantId(restaurantId), new DishId(dishId));

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

        dishService.setDishAvailability(new RestaurantId(restaurantId), new DishId(dishId), dto.available());

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

    @PostMapping("/restaurants/{restaurantId}/schedule_publish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void schedulePublish(
            @PathVariable final UUID restaurantId,
            @RequestBody SchedulePublishDto dto) {
        log.info("Scheduling publish of all draft dishes for restaurant {} at {}", restaurantId, dto.publishAt());
        dishService.schedulePublishAllDraftDishes(new RestaurantId(restaurantId), dto.publishAt());
    }



}
