import { load, save } from "../infrastructure/storage.ts";

const KEY = "kdg.home-address";

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
	return load<CustomerProfile>(KEY);
}

export function saveProfile(profile: CustomerProfile): void {
	save(KEY, profile);
}

export function savedHomeAddress(): HomeAddress | null {
	return savedProfile()?.address ?? null;
}
