package be.kdg.backend.api.dto;

import java.util.UUID;

public record SelfAssignRequest(UUID deliveryId, UUID driverId) {}