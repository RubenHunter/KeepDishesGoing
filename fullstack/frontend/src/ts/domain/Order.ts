export interface OrderItemRequest {
    menuItemId: string;
    itemName: string;
    quantity: number;
    unitPrice: number;
}
export interface CreateOrderRequest {
    customerId: string;
    restaurantId: string;
    deliveryAddress: string;
    customerEmail: string;
    items: OrderItemRequest[];
}
export interface OrderResponse {
    orderId: string;
    message: string;
    status: string;
}
