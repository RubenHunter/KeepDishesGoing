package be.kdg.sa.backend.application;

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

