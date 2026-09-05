package be.kdg.backend.api.dto;

import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID cartId,
        UUID customerId,
        UUID restaurantId,
        List<CartItemResponse> items,
        double total,
        String currency
) {
    public record CartItemResponse(
            UUID menuItemId,
            String itemName,
            int quantity,
            double unitPrice,
            double lineTotal
    ) {}
}