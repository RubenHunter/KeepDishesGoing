import { load, remove, save } from "../infrastructure/storage.ts";

const KEY = "kdg.last-order";

export function lastTrackedOrderId(): string | null {
	return load<string>(KEY);
}

export function trackOrder(orderId: string): void {
	save(KEY, orderId);
}

export function clearTrackedOrder(): void {
	remove(KEY);
}
