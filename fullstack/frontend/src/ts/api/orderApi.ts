import { API } from "../config.ts";
import type {
	CheckoutResponse,
	OrderDetail,
	Tracking,
} from "../domain/Order.ts";
import { request } from "./http.ts";

const base = `${API.order}/orders`;

export type CheckoutRequest = {
	cartId: string;
	customerId: string;
	customerName: string;
	street: string;
	number: string;
	postalCode: string;
	city: string;
	country: string;
	email: string;
};

/** US17/US18 - server validates items + prices, then locks content and price. */
export function checkout(body: CheckoutRequest): Promise<CheckoutResponse> {
	return request(`${base}/checkout`, { method: "POST", body });
}

/** US20 - stub payment provider confirmation (webhook endpoint accepts POST). */
export function confirmPayment(paymentRef: string): Promise<void> {
	return request(`${API.order}/payments/${encodeURIComponent(paymentRef)}/confirm`, {
		method: "POST",
	});
}

/** Places the order after (stub) payment - starts the 5-min decision window (US23). */
export function placeOrder(orderId: string): Promise<void> {
	return request(`${base}/${orderId}/place`, { method: "POST" });
}

/** Customer cancellation while PLACED/ACCEPTED. */
export function cancelOrder(orderId: string, reason: string): Promise<void> {
	return request(`${base}/${orderId}/cancel`, { method: "POST", body: { reason } });
}

export function getOrder(orderId: string): Promise<OrderDetail> {
	return request(`${base}/${orderId}`);
}

/** US21/US33 - tracking read model (status + lifecycle timestamps + events). */
export function getTracking(orderId: string): Promise<Tracking> {
	return request(`${base}/${orderId}/tracking`);
}

export type OrderSummary = {
	orderId: string;
	customerName: string;
	status: string;
	totalAmount: number;
	currency: string;
	placedAt: string | null;
	itemCount: number;
	deliveryAddress: string | null;
	items: { menuItemId: string; itemName: string; quantity: number; unitPrice: number }[];
};

export function listOrdersByRestaurant(restaurantId: string): Promise<OrderSummary[]> {
	return request(`${base}/restaurant/${restaurantId}`);
}
