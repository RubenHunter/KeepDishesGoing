package be.kdg.backend.api;

import be.kdg.backend.api.dto.*;
import be.kdg.backend.application.DishService;
import be.kdg.backend.domain.dish.*;
import be.kdg.backend.domain.restaurant.RestaurantId;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
     /restaurants/{id}/dishes
     post, owner, req: CreateDishDto,
     get, owner, resp: List<DishDto>
    */
    @PostMapping("/restaurants/{id}/dishes")
    @PreAuthorize("@ownerGuard.canManageRestaurant(#id)")
    public ResponseEntity<DishDto> createDish(@Valid @RequestBody CreateDishDto dto, @PathVariable final UUID id) {
        final RestaurantId restaurantId = new RestaurantId(id);

        DishCategory categoryEnum = (dto.category() == null || dto.category().isBlank())
                ? null
                : DishCategory.valueOf(dto.category().toUpperCase(Locale.ROOT));

        DishId createdId = dishService.createDraftDish(
                restaurantId,
                new DishName(dto.name()),
                new Description(dto.description()),
                dto.price(),
                categoryEnum,
                dto.imageUrl()
        );

        return ResponseEntity.created(
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{dishId}")
                        .buildAndExpand(createdId.id())
                        .toUri()
        ).build();
    }

    // GET /restaurants/{id}/dishes -> service returns DTOs
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
    @PreAuthorize("@ownerGuard.canManageRestaurant(#restaurantId)")
    public ResponseEntity<DishDto> updateDish(@Valid @RequestBody UpdateDishDto dto, @PathVariable final UUID restaurantId, @PathVariable final UUID dishId) {
        log.info("Updating dish with id {} from restaurant with id {}: {}", dishId, restaurantId, dto);
        DishDto updated = dishService.updateDraftDish(new RestaurantId(restaurantId), new DishId(dishId), dto);
        return ResponseEntity.ok(updated);
    }

    /*
     Canonical lifecycle transition (mistake #16 — resource PATCH /status).
     status=PUBLISHED -> publish; DRAFT -> de-publish; available!=null -> US9 out-of-stock.
    */
    @PatchMapping("/restaurants/{restaurantId}/dishes/{dishId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@ownerGuard.canManageRestaurant(#restaurantId)")
    public void updateDishStatus(
            @PathVariable final UUID restaurantId,
            @PathVariable final UUID dishId,
            @RequestBody DishStatusUpdateDto dto) {
        final RestaurantId rid = new RestaurantId(restaurantId);
        final DishId did = new DishId(dishId);
        String status = dto.status() == null ? "" : dto.status().toUpperCase(Locale.ROOT);
        switch (status) {
            case "PUBLISHED" -> dishService.publishDish(rid, did);
            case "DRAFT" -> dishService.dePublishDish(rid, did);
            case "OUT_OF_STOCK", "AVAILABLE" -> {
                if (dto.available() == null) {
                    throw new IllegalArgumentException("Field 'available' is required for availability updates");
                }
                dishService.setDishAvailability(rid, did, dto.available());
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported status '" + dto.status()
                            + "' — expected PUBLISHED|DRAFT|OUT_OF_STOCK|AVAILABLE");
        }
    }

    /*
    Menu publication as a created sub-resource (mistake #16 — no verbs):
      POST /restaurants/{id}/menu/publications                       -> apply all pending drafts now (US7)
      POST /restaurants/{id}/menu/publications {"publishAt": ...}    -> schedule the batch for later (US8)
    */
    @PostMapping("/restaurants/{restaurantId}/menu/publications")
    @PreAuthorize("@ownerGuard.canManageRestaurant(#restaurantId)")
    public ResponseEntity<MenuPublicationResponse> publishMenu(
            @PathVariable final UUID restaurantId,
            @RequestBody(required = false) SchedulePublishDto body) {
        boolean scheduled = body != null && body.publishAt() != null;
        if (scheduled) {
            log.info("Scheduling publish of all draft dishes for restaurant {} at {}", restaurantId, body.publishAt());
            dishService.schedulePublishAllDraftDishes(new RestaurantId(restaurantId), body.publishAt());
            return ResponseEntity.accepted().body(new MenuPublicationResponse(true, "SCHEDULED", body.publishAt()));
        }
        log.info("Publishing all draft dishes for restaurant {}", restaurantId);
        dishService.publishAllDraftDishes(new RestaurantId(restaurantId));
        return ResponseEntity.status(HttpStatus.CREATED).body(new MenuPublicationResponse(false, "PUBLISHED", null));
    }

    /** Result of a menu publication request. */
    public record MenuPublicationResponse(boolean scheduled, String state, java.time.LocalDateTime scheduledAt) {}



}
