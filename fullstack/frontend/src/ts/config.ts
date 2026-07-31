/** Central service + Keycloak config. Proxied paths - see vite.config.ts. */

export const API = {
	restaurant: "/restaurant-api/api",
	order: "/order-api/api",
	delivery: "/delivery-api/api",
} as const;

export const KEYCLOAK = {
	url: "http://localhost:8180",
	realm: "keepdishesgoing",
	/**
	 * Demo setup: confidential client with direct access grants enabled
	 * (see app-security/Setup_Keycloak.md). Secret ships with the demo frontend -
	 * acceptable for a local course demo, never for production.
	 */
	clientId: "restaurant-service",
	clientSecret: "upJ1DRbuRkHL3RKxRy0ib0ZJ4mkNIjWS",
} as const;

export function tokenEndpoint(): string {
	return `${KEYCLOAK.url}/realms/${KEYCLOAK.realm}/protocol/openid-connect/token`;
}

export const TRACKING_POLL_MS = 5000;
export const ORDER_DECISION_WINDOW_MIN = 5;

/** US36 - payout policy (base + per-minute, min/max billed minutes). */
export const PAYOUT_POLICY = {
	baseFee: 3.0,
	perMinute: 0.3,
	minMinutes: 5,
	maxMinutes: 30,
} as const;
