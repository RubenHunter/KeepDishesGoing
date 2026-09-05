import { savedHomeAddress } from "../state/homeAddress.ts";
import { savedGeneralLocation } from "../state/location.ts";

interface LatLon { lat: number; lon: number }

const cache = new Map<string, LatLon | null>();

/** Nominatim usage policy: max 1 req/s. Space requests 1.1s apart. */
const MIN_INTERVAL_MS = 1100;
let chain: Promise<unknown> = Promise.resolve();

function throttled<T>(fn: () => Promise<T>): Promise<T> {
	const run = chain.then(fn, fn);
	chain = run.then(
		() => new Promise((r) => setTimeout(r, MIN_INTERVAL_MS)),
		() => new Promise((r) => setTimeout(r, MIN_INTERVAL_MS)),
	);
	return run;
}

/**
 * Geocode a single-line address via Nominatim. Results cached in-memory.
 * Returns null if offline or no result.
 */
export async function geocode(address: string): Promise<LatLon | null> {
	const cached = cache.get(address);
	if (cached !== undefined) return cached;

	try {
		const result = await throttled(async () => {
			const url = new URL("https://nominatim.openstreetmap.org/search");
			url.searchParams.set("q", address);
			url.searchParams.set("format", "json");
			url.searchParams.set("limit", "1");

			const res = await fetch(url, { headers: { "Accept-Language": "en" } });
			if (!res.ok) throw new Error("Nominatim error");
			const data = await res.json();
			if (data.length === 0) return null;
			return { lat: parseFloat(data[0].lat), lon: parseFloat(data[0].lon) } as LatLon;
		});
		if (result !== null) cache.set(address, result);
		return result;
	} catch {
		// Don't cache failures - transient errors (rate limit/offline) should retry.
		return null;
	}
}

/** Haversine distance in km. */
export function haversineKm(a: LatLon, b: LatLon): number {
	const R = 6371;
	const dLat = (b.lat - a.lat) * Math.PI / 180;
	const dLon = (b.lon - a.lon) * Math.PI / 180;
	const sinLat = Math.sin(dLat / 2);
	const sinLon = Math.sin(dLon / 2);
	const h = sinLat * sinLat + Math.cos(a.lat * Math.PI / 180) * Math.cos(b.lat * Math.PI / 180) * sinLon * sinLon;
	return 2 * R * Math.asin(Math.sqrt(h));
}

/** Format a HomeAddress as single-line string. */
export function homeAddressLine(): string | null {
	const a = savedHomeAddress();
	if (!a || !a.street) return null;
	return `${a.street} ${a.number}, ${a.postalCode} ${a.city}, ${a.country}`;
}

/** Get home coordinates from saved address, geocoding if needed. */
export async function homeCoordinates(): Promise<LatLon | null> {
	const line = homeAddressLine();
	if (!line) return null;
	return geocode(line);
}

/**
 * Origin for distance-based features: full home address, else the general
 * location (postcode/city) the user entered, else null.
 */
export function distanceOriginLine(): string | null {
	return homeAddressLine() ?? savedGeneralLocation();
}

/** Geocoded origin for distance features (home address or general location). */
export async function distanceOriginCoordinates(): Promise<LatLon | null> {
	const line = distanceOriginLine();
	if (!line) return null;
	return geocode(line);
}
