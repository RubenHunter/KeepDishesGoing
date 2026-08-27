import { decodeJwt, isExpired, rolesOf, type JwtPayload } from "../infrastructure/jwt.ts";
import { load, remove, save } from "../infrastructure/storage.ts";
import { KEYCLOAK, tokenEndpoint } from "../config.ts";

export type Role = "owner" | "driver" | "admin" | "user";

export type Session = {
	token: string;
	sub: string;
	username: string;
	roles: string[];
	/** Epoch ms when the access token expires. */
	expiresAt: number;
};

const STORAGE_KEY = "kdg.session";
const REFRESH_KEY = "kdg.refreshToken";

let current: Session | null = restore();
const listeners = new Set<() => void>();

/** Refresh-token grant singleton — many parallel 401-prone calls share one refresh. */
let refreshInFlight: Promise<Session | null> | null = null;

function restore(): Session | null {
	const token = load<string>(STORAGE_KEY);
	if (!token) return null;
	const payload = decodeJwt(token);
	if (!payload || isExpired(payload)) {
		remove(STORAGE_KEY);
		return null;
	}
	return toSession(token, payload);
}

function toSession(token: string, payload: JwtPayload): Session {
	return {
		token,
		sub: payload.sub,
		username: payload.preferred_username ?? payload.email ?? payload.sub,
		roles: rolesOf(payload),
		expiresAt: (payload.exp ?? 0) * 1000,
	};
}

/** Synchronous read — only safe for rendering; network calls must use ensureSession(). */
export function getSession(): Session | null {
	return current;
}

/**
 * Returns a session with a still-valid access token, silently refreshing it via the
 * stored refresh_token when it is about to expire. Returns null when logged out or
 * when the refresh fails (caller should treat that as "please log in again").
 */
export async function ensureSession(): Promise<Session | null> {
	if (!current) return null;
	if (current.expiresAt - Date.now() > 30_000) return current;

	if (!refreshInFlight) {
		refreshInFlight = refresh().finally(() => {
			refreshInFlight = null;
		});
	}
	return refreshInFlight;
}

async function refresh(): Promise<Session | null> {
	const refreshToken = load<string>(REFRESH_KEY);
	if (!refreshToken) return current;

	let response: Response;
	try {
		response = await fetch(tokenEndpoint(), {
			method: "POST",
			headers: { "Content-Type": "application/x-www-form-urlencoded" },
			body: new URLSearchParams({
				grant_type: "refresh_token",
				client_id: KEYCLOAK.clientId,
				client_secret: KEYCLOAK.clientSecret,
				refresh_token: refreshToken,
			}),
		});
	} catch {
		return current; // IdP briefly unreachable — keep last-known token.
	}
	if (!response.ok) {
		clearSession();
		return null;
	}
	const data = (await response.json()) as { access_token?: string; refresh_token?: string };
	if (!data.access_token) {
		clearSession();
		return null;
	}
	if (data.refresh_token) save(REFRESH_KEY, data.refresh_token);
	return setToken(data.access_token);
}

export function hasRole(role: Role): boolean {
	return current !== null && current.roles.includes(role);
}

export function setTokens(accessToken: string, refreshToken?: string): Session {
	if (refreshToken) save(REFRESH_KEY, refreshToken);
	else remove(REFRESH_KEY);
	return setToken(accessToken);
}

export function setToken(token: string): Session {
	const payload = decodeJwt(token);
	if (!payload) throw new Error("Invalid token received from identity provider");
	current = toSession(token, payload);
	save(STORAGE_KEY, token);
	notify();
	return current;
}

export function clearSession(): void {
	current = null;
	remove(STORAGE_KEY);
	remove(REFRESH_KEY);
	notify();
}

export function onSessionChange(listener: () => void): () => void {
	listeners.add(listener);
	return () => listeners.delete(listener);
}

function notify(): void {
	for (const l of listeners) l();
}
