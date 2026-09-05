import { distanceOriginCoordinates, geocode, haversineKm } from "../../../presenter/geo.ts";
import type { CardModel } from "./filters.ts";

/**
 * Resolve each restaurant's distance from the origin (home address or general
 * location), caching into the given map. Returns true when any restaurant
 * turned out to be beyond `maxKm` (so the caller can re-render and drop them).
 */
export async function computeDistances(
	list: CardModel[],
	km: Map<string, number>,
	maxKm: number,
	isDestroyed: () => boolean,
): Promise<boolean> {
	const origin = await distanceOriginCoordinates();
	if (!origin || isDestroyed()) return false;

	let changed = false;
	for (const r of list) {
		if (isDestroyed()) break;
		if (!r.fullAddress || km.has(r.id)) continue;
		const coord = await geocode(r.fullAddress);
		if (!coord) continue;
		const distance = haversineKm(origin, coord);
		km.set(r.id, distance);
		if (distance > maxKm) changed = true;
	}
	return changed;
}
