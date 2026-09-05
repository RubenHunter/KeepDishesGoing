import { getMyRestaurantId } from "../api/restaurantApi.ts";
import { load, save } from "../infrastructure/storage.ts";
import { getSession } from "./session.ts";

/**
 * Owner ↔ restaurant link. Remembered per Keycloak subject after creation
 * (Location header) or discovery via GET /api/restaurants/mine (US1).
 */

function key(): string | null {
	const sub = getSession()?.sub;
	return sub ? `kdg.owner.restaurant.${sub}` : null;
}

export function ownerRestaurantId(): string | null {
	const k = key();
	return k ? load<string>(k) : null;
}

export function rememberRestaurant(id: string): void {
	const k = key();
	if (k) save(k, id);
}

/** Remembered id, falling back to backend discovery by owner subject. */
export async function resolveOwnerRestaurantId(): Promise<string | null> {
	const remembered = ownerRestaurantId();
	if (remembered) return remembered;
	const discovered = await getMyRestaurantId();
	if (discovered) rememberRestaurant(discovered);
	return discovered;
}
