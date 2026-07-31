import { load, save } from "./storage.ts";

/**
 * Recent order ids placed from this browser. Lets the owner console surface
 * incoming orders without a list endpoint (demo glue - same-device demo flow).
 */

const KEY = "kdg.recentOrders";
const MAX_ENTRIES = 20;

export function rememberOrder(orderId: string): void {
	const ids = load<string[]>(KEY) ?? [];
	const next = [orderId, ...ids.filter((id) => id !== orderId)].slice(0, MAX_ENTRIES);
	save(KEY, next);
}

export function recentOrderIds(): string[] {
	return load<string[]>(KEY) ?? [];
}
