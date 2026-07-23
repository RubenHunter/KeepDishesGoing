package be.kdg.backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCartRequest(
        @NotNull UUID customerId
) {}