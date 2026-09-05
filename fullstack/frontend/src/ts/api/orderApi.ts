import { API, PAYMENT_WEBHOOK } from "../config.ts";
import type {
	CheckoutResponse,
	OrderDetail,
	Tracking,
} from "../domain/Order.ts";
import { request } from "./http.ts";

const base = `${API.order}/orders`;

export type CheckoutRequest = {
	cartId: string;
	customerName: string;
	street: string;
	number: string;
	postalCode: string;
	city: string;
	country: string;
	email: string;
};

/** US17/US18 - create checkout (resource POST /orders); validates + locks content and price. */
export function checkout(body: CheckoutRequest): Promise<CheckoutResponse> {
	return request(base, { method: "POST", body, auth: true });
}

/** US20 - stub payment provider confirmation (webhook PATCH /payments/{ref}/status). */
export function confirmPayment(paymentRef: string): Promise<void> {
	return request(`${API.order}/payments/${encodeURIComponent(paymentRef)}/status`, {
		method: "PATCH",
		headers: { [PAYMENT_WEBHOOK.header]: PAYMENT_WEBHOOK.secret },
	});
}

/** US18 - place after payment PAID; starts the 5-min decision window (US23). */
export function placeOrder(orderId: string): Promise<void> {
	return setOrderStatus(orderId, "PLACED");
}

/** Customer cancellation while PLACED/ACCEPTED. */
export function cancelOrder(orderId: string, reason: string): Promise<void> {
	return setOrderStatus(orderId, "CANCELLED", reason);
}

/** One lifecycle endpoint (PATCH /orders/{id}/status). */
export function setOrderStatus(
	orderId: string,
	status: "PLACED" | "CANCELLED",
	reason?: string,
): Promise<void> {
	return request(`${base}/${orderId}/status`, { method: "PATCH", body: { status, reason }, auth: true });
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
	restaurantId: string;
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
	return request(`${base}/restaurant/${restaurantId}`, { auth: true });
}

/** All orders for the current account — identity is derived from the JWT subject server-side. */
export function listOrdersByCustomer(_customerId?: string): Promise<OrderSummary[]> {
	return request(`${base}/customer`, { auth: true });
}
