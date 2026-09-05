package be.kdg.backend.api.dto;

import be.kdg.backend.domain.Price;
import be.kdg.backend.domain.dish.DishCategory;

public record UpdateDishDto(
        String name,
        String description,
        Price price,
        DishCategory category,
        String imageUrl
) {}

