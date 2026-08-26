import { API } from "../config.ts";
import { request } from "./http.ts";

const base = `${API.order}/customers`;

export type BackendProfile = {
	name: string;
	email: string;
	street: string;
	number: string;
	postalCode: string;
	city: string;
	country: string;
};

/** Fetch the account profile for the given Keycloak subject (404 → no profile yet). */
export function getCustomerProfile(customerId: string): Promise<BackendProfile> {
	return request(`${base}/${customerId}`);
}

/** Upsert the account profile (account settings follow the user across devices). */
export function saveCustomerProfile(customerId: string, body: BackendProfile): Promise<void> {
	return request(`${base}/${customerId}`, { method: "PUT", body });
}
