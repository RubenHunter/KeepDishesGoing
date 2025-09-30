package be.kdg.sa.backend.application;

import java.util.List;

public record CreateOrderCommand(
        String customerId,
        String restaurantId,
        String deliveryAddress,
        String customerEmail,
        List<OrderItemCommand> items
) {
    public record OrderItemCommand(
            String menuItemId,
            String itemName,
            int quantity,
            double unitPrice
    ) {
        public OrderItemCommand {
            if (menuItemId == null || menuItemId.isBlank()) {
                throw new IllegalArgumentException("MenuItem ID cannot be null or empty");
            }
            if (itemName == null || itemName.isBlank()) {
                throw new IllegalArgumentException("Item name cannot be null or empty");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            if (unitPrice <= 0) {
                throw new IllegalArgumentException("Unit price must be positive");
            }
        }
    }

    public CreateOrderCommand {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        if (restaurantId == null || restaurantId.isBlank()) {
            throw new IllegalArgumentException("Restaurant ID cannot be null or empty");
        }
        if (deliveryAddress == null || deliveryAddress.isBlank()) {
            throw new IllegalArgumentException("Delivery address cannot be null or empty");
        }
        if (customerEmail == null || customerEmail.isBlank()) {
            throw new IllegalArgumentException("Customer email cannot be null or empty");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
    }
}