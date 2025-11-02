import { getOrCreateCustomerId } from "../state/session";

export interface Cart {
    cartId: string;
    customerId: string;
    restaurantId: string | null;
    items: CartItem[];
    totalAmount: number;
    currency: string;
}
export interface CartItem {
    menuItemId: string;
    itemName: string;
    quantity: number;
    unitPrice: number;
    lineTotal: number;
    currency: string;
}

const ORDER_BASE = "/order-api/api";

export async function getCart(customerId = getOrCreateCustomerId()): Promise<Cart> {
    const res = await fetch(`${ORDER_BASE}/cart/${customerId}`);
    if (!res.ok) throw new Error("Failed to fetch cart");
    return res.json();
}

export async function addItemToCart(params: {
    customerId?: string;
    restaurantId: string;
    menuItemId: string;
    itemName: string;
    quantity: number;
    unitPrice: number;
}): Promise<void> {
    const cid = params.customerId ?? getOrCreateCustomerId();
    const res = await fetch(`${ORDER_BASE}/cart/${cid}/items`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            menuItemId: params.menuItemId,
            itemName: params.itemName,
            quantity: params.quantity,
            unitPrice: params.unitPrice,
            restaurantId: params.restaurantId,
        }),
    });
    if (!res.ok) throw new Error("Failed to add item");
}

export async function updateCartQuantity(params: {
    customerId?: string;
    menuItemId: string;
    quantity: number;
}): Promise<void> {
    const cid = params.customerId ?? getOrCreateCustomerId();
    const res = await fetch(`${ORDER_BASE}/cart/${cid}/items/${params.menuItemId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ quantity: params.quantity }),
    });
    if (!res.ok) throw new Error("Failed to update quantity");
}

export async function removeFromCart(params: {
    customerId?: string;
    menuItemId: string;
}): Promise<void> {
    const cid = params.customerId ?? getOrCreateCustomerId();
    const res = await fetch(`${ORDER_BASE}/cart/${cid}/items/${params.menuItemId}`, { method: "DELETE" });
    if (!res.ok) throw new Error("Failed to remove item");
}

export async function clearCart(customerId = getOrCreateCustomerId()): Promise<void> {
    const res = await fetch(`${ORDER_BASE}/cart/${customerId}`, { method: "DELETE" });
    if (!res.ok) throw new Error("Failed to clear cart");
}

export interface CreateOrderItemRequest {
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
    items: CreateOrderItemRequest[];
}
export interface OrderResponse {
    orderId: string;
    message: string;
    status: string;
}

export async function createOrder(req: CreateOrderRequest): Promise<OrderResponse> {
    const res = await fetch(`${ORDER_BASE}/orders`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(req),
    });
    if (!res.ok) throw new Error("Failed to create order");
    return res.json();
}

export async function placeOrderValidated(orderId: string): Promise<OrderResponse> {
    const res = await fetch(`${ORDER_BASE}/orders/${orderId}/place-validated`, { method: "POST" });
    if (!res.ok) throw new Error("Failed to place order");
    return res.json();
}
