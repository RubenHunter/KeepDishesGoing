import { listOrdersByCustomer } from "../api/orderApi.ts";
import { getSession } from "./session.ts";

/**
 * Cached "does this account have any orders?" flag, backed by the order-service
 * (keyed by Keycloak subject) so the nav link reflects real order history on
 * any device, not just this browser.
 */
let orderCount = 0;
let lastSub: string | null = null;
const listeners = new Set<() => void>();

function notify(): void {
	for (const l of listeners) l();
}

export function hasOrders(): boolean {
	return orderCount > 0;
}

export function refreshMyOrders(): void {
	const sub = getSession()?.sub ?? null;
	if (!sub) {
		if (orderCount !== 0) {
			orderCount = 0;
			notify();
		}
		lastSub = null;
		return;
	}
	if (sub !== lastSub) {
		orderCount = 0;
		lastSub = sub;
		notify();
	}
	void listOrdersByCustomer(sub)
		.then((orders) => {
			if (lastSub === sub) {
				orderCount = orders.length;
				notify();
			}
		})
		.catch(() => {});
}

export function onMyOrdersChange(listener: () => void): () => void {
	listeners.add(listener);
	return () => listeners.delete(listener);
}
