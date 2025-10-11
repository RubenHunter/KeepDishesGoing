package be.kdg.backend.api.dto;

import be.kdg.backend.domain.Price;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDishDto(
        @NotBlank(message = "A Dish needs a name")
        @Size(min = 2, max = 100, message = "Dish name must be between 2 and 100 characters")
        String name,
        @Size(max = 400, message = "Description cannot be longer than 400 characters")
        String description,
        Price price,
        String category
) {}
