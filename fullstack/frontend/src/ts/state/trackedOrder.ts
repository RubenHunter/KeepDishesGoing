import { load, remove, save } from "../infrastructure/storage.ts";
import { getSession } from "./session.ts";

/** Tracked order is scoped per Keycloak subject (guests share the "guest" scope). */
function key(): string {
	return `kdg.last-order.${getSession()?.sub ?? "guest"}`;
}

const listeners = new Set<() => void>();

export function lastTrackedOrderId(): string | null {
	return load<string>(key());
}

export function trackOrder(orderId: string): void {
	save(key(), orderId);
	for (const l of listeners) l();
}

export function clearTrackedOrder(): void {
	remove(key());
	for (const l of listeners) l();
}

export function onTrackedOrderChange(listener: () => void): () => void {
	listeners.add(listener);
	return () => listeners.delete(listener);
}
