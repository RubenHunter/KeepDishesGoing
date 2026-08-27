import { getCustomerProfile, saveCustomerProfile } from "../api/customerApi.ts";
import { load, save } from "../infrastructure/storage.ts";
import { getSession } from "./session.ts";

/**
 * Account profile (name, contact email, home address).
 * Persisted in the order-service (keyed by Keycloak subject) so it follows the
 * user across devices; a browser-local mirror keeps reads synchronous and
 * supports guest (anonymous) users.
 */

export interface HomeAddress {
	street: string;
	number: string;
	postalCode: string;
	city: string;
	country: string;
}

export interface CustomerProfile {
	name: string;
	email: string;
	address: HomeAddress;
}

function key(): string {
	return `kdg.home-address.${getSession()?.sub ?? "guest"}`;
}

function toLocal(p: { name: string; email: string; street: string; number: string; postalCode: string; city: string; country: string }): CustomerProfile {
	return {
		name: p.name ?? "",
		email: p.email ?? "",
		address: {
			street: p.street ?? "",
			number: p.number ?? "",
			postalCode: p.postalCode ?? "",
			city: p.city ?? "",
			country: p.country ?? "",
		},
	};
}

function toBackend(p: CustomerProfile): { name: string; email: string; street: string; number: string; postalCode: string; city: string; country: string } {
	return { name: p.name, email: p.email, ...p.address };
}

/** Synchronous cached read (localStorage mirror). */
export function savedProfile(): CustomerProfile | null {
	return load<CustomerProfile>(key());
}

export function savedHomeAddress(): HomeAddress | null {
	return savedProfile()?.address ?? null;
}

/** Fetch the profile from the backend and refresh the local mirror. Guests → null. */
export async function loadProfile(): Promise<CustomerProfile | null> {
	const sub = getSession()?.sub;
	if (!sub) return null;
	try {
		const profile = toLocal(await getCustomerProfile());
		save(key(), profile);
		return profile;
	} catch {
		return null; // 404 (no profile yet) or offline
	}
}

/** Save to the backend (when logged in) + local mirror (instant UI / guests). */
export async function saveProfile(profile: CustomerProfile): Promise<void> {
	save(key(), profile);
	const sub = getSession()?.sub;
	if (!sub) return;
	try {
		await saveCustomerProfile(toBackend(profile));
	} catch {
		/* backend unavailable — profile stays local */
	}
}
