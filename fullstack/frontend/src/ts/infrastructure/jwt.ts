/** Minimal JWT payload decode - no library, no signature check (server verifies). */

export type JwtPayload = {
	sub: string;
	preferred_username?: string;
	email?: string;
	realm_access?: { roles?: string[] };
	exp?: number;
};

export function decodeJwt(token: string): JwtPayload | null {
	const parts = token.split(".");
	if (parts.length !== 3) return null;
	try {
		const payload = parts[1].replace(/-/g, "+").replace(/_/g, "/");
		return JSON.parse(atob(payload)) as JwtPayload;
	} catch {
		return null;
	}
}

export function rolesOf(payload: JwtPayload | null): string[] {
	return payload?.realm_access?.roles ?? [];
}

export function isExpired(payload: JwtPayload | null): boolean {
	if (!payload?.exp) return false;
	return payload.exp * 1000 <= Date.now();
}
