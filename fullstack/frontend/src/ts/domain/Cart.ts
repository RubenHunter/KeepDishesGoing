export interface CartItem {
    menuItemId: string;
    itemName: string;
    quantity: number;
    unitPrice: number;
    currency: string;
    lineTotal: number;
}
export interface Cart {
    cartId: string;
    customerId: string;
    restaurantId: string | null;
    items: CartItem[];
    totalAmount: number;
    currency: string;
}