package be.kdg.sa.backend;

import be.kdg.sa.backend.api.dto.MenuItemValidationResponse;
import be.kdg.sa.backend.api.dto.RestaurantStatusResponse;
import be.kdg.sa.backend.application.OrderValidationService;
import be.kdg.sa.backend.domain.Order.*;
import be.kdg.sa.backend.domain.Shared.Money;
import be.kdg.sa.backend.domain.Shared.Quantity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderValidationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OrderValidationService orderValidationService;

    private Order validOrder;

    @BeforeEach
    void setUp() {
        validOrder = new Order(
                OrderId.of("ORD-TEST-123"),
                CustomerId.of("CUST-TEST-123"),
                RestaurantId.of("a6a52c73-9070-4128-a988-255383b941bc"),
                "Test Address",
                "test@example.com"
        );

        validOrder.addItem(
                MenuItemId.of("4ac746dc-7943-479e-b3ee-d158b9f15260"),
                "Lasagna",
                Quantity.of(2),
                Money.ofEuros(14.5)
        );
    }

    @Test
    void whenRestaurantOpenAndValidItems_thenValidationSucceeds() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(RestaurantStatusResponse.class), anyString()))
                .thenReturn(new RestaurantStatusResponse(true, "ACTIVE", "Restaurant is open"));

        when(restTemplate.postForObject(anyString(), any(), eq(MenuItemValidationResponse.class), anyString(), anyString()))
                .thenReturn(new MenuItemValidationResponse(true, "Validation successful", 14.5, "EUR", true));

        // When
        OrderValidationService.ValidationResult result = orderValidationService.validateOrderBeforeCheckout(validOrder);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.message()).isEqualTo("Validation successful");
    }

    @Test
    void whenRestaurantClosed_thenValidationFails() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(RestaurantStatusResponse.class), anyString()))
                .thenReturn(new RestaurantStatusResponse(false, "CLOSED", "Restaurant is closed"));

        // When
        OrderValidationService.ValidationResult result = orderValidationService.validateOrderBeforeCheckout(validOrder);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.message()).isEqualTo("Restaurant is currently closed");
    }

    @Test
    void whenPriceMismatch_thenValidationFails() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(RestaurantStatusResponse.class), anyString()))
                .thenReturn(new RestaurantStatusResponse(true, "ACTIVE", "Restaurant is open"));

        when(restTemplate.postForObject(anyString(), any(), eq(MenuItemValidationResponse.class), anyString(), anyString()))
                .thenReturn(new MenuItemValidationResponse(false, "Price has changed", 15.0, "EUR", true));

        // When
        OrderValidationService.ValidationResult result = orderValidationService.validateOrderBeforeCheckout(validOrder);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.message()).contains("Price has changed");
    }
}