import { API } from "../config.ts";
import type { ServerCart } from "../domain/Cart.ts";
import type { Eur } from "../domain/types.ts";
import { request } from "./http.ts";

const base = `${API.order}/carts`;

/** Server-side cart (order-service). One cart per customer, one restaurant per cart (US16). */

export function createCart(): Promise<ServerCart> {
	return request(base, { method: "POST", auth: true });
}

export function getCart(cartId: string): Promise<ServerCart> {
	return request(`${base}/${cartId}`, { auth: true });
}

export function addItem(
	cartId: string,
	item: {
		menuItemId: string;
		itemName: string;
		quantity: number;
		unitPrice: Eur;
		restaurantId: string;
	},
): Promise<ServerCart> {
	return request(`${base}/${cartId}/items`, { method: "POST", body: item, auth: true });
}

export function updateItemQuantity(
	cartId: string,
	menuItemId: string,
	quantity: number,
): Promise<ServerCart> {
	return request(`${base}/${cartId}/items/${menuItemId}`, {
		method: "PATCH",
		body: { quantity },
		auth: true,
	});
}

export function removeItem(cartId: string, menuItemId: string): Promise<void> {
	return request(`${base}/${cartId}/items/${menuItemId}`, { method: "DELETE", auth: true });
}

export function clearCart(cartId: string): Promise<void> {
	return request(`${base}/${cartId}`, { method: "DELETE", auth: true });
}
