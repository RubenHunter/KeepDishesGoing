import { load, save } from "../infrastructure/storage.ts";

const KEY = "kdg.scheduled-publish";

export function getScheduledPublish(restaurantId: string): string | null {
	const map = load<Record<string, string>>(KEY) ?? {};
	return map[restaurantId] ?? null;
}

export function setScheduledPublish(restaurantId: string, iso: string): void {
	const map = load<Record<string, string>>(KEY) ?? {};
	map[restaurantId] = iso;
	save(KEY, map);
}

export function clearScheduledPublish(restaurantId: string): void {
	const map = load<Record<string, string>>(KEY) ?? {};
	delete map[restaurantId];
	save(KEY, map);
}
