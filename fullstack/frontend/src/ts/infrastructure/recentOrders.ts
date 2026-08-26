import { getSession } from "../state/session.ts";
import { load, save } from "./storage.ts";

/**
 * Recent order ids placed by THIS account. Scoped per Keycloak subject
 * (guests share the "guest" scope) so history persists across logout/login
 * without leaking into other accounts.
 */

const MAX_ENTRIES = 20;

function key(): string {
	return `kdg.recentOrders.${getSession()?.sub ?? "guest"}`;
}

export function rememberOrder(orderId: string): void {
	const ids = load<string[]>(key()) ?? [];
	const next = [orderId, ...ids.filter((id) => id !== orderId)].slice(0, MAX_ENTRIES);
	save(key(), next);
}

export function recentOrderIds(): string[] {
	return load<string[]>(key()) ?? [];
}
