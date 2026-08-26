import { load, save } from "../infrastructure/storage.ts";
import { getSession } from "./session.ts";

/** Profile is scoped per Keycloak subject (guests share the "guest" scope). */
function key(): string {
	return `kdg.home-address.${getSession()?.sub ?? "guest"}`;
}

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

export function savedProfile(): CustomerProfile | null {
	return load<CustomerProfile>(key());
}

export function saveProfile(profile: CustomerProfile): void {
	save(key(), profile);
}

export function savedHomeAddress(): HomeAddress | null {
	return savedProfile()?.address ?? null;
}
