import { savedHomeAddress } from "../state/homeAddress.ts";

interface LatLon { lat: number; lon: number }

const cache = new Map<string, LatLon | null>();

/**
 * Geocode a single-line address via Nominatim. Results cached in-memory.
 * Returns null if offline or no result.
 */
export async function geocode(address: string): Promise<LatLon | null> {
	const cached = cache.get(address);
	if (cached !== undefined) return cached;

	try {
		const url = new URL("https://nominatim.openstreetmap.org/search");
		url.searchParams.set("q", address);
		url.searchParams.set("format", "json");
		url.searchParams.set("limit", "1");

		const res = await fetch(url, { headers: { "Accept-Language": "en" } });
		if (!res.ok) throw new Error("Nominatim error");
		const data = await res.json();
		if (data.length === 0) {
			cache.set(address, null);
			return null;
		}
		const r: LatLon = { lat: parseFloat(data[0].lat), lon: parseFloat(data[0].lon) };
		cache.set(address, r);
		return r;
	} catch {
		cache.set(address, null);
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
