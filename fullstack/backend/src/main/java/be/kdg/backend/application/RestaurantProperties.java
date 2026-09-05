package be.kdg.backend.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Outbound HTTP config to restaurant-service.
 */
@ConfigurationProperties(prefix = "kdg.restaurant")
public record RestaurantProperties(String baseUrl, String apiBase) {}