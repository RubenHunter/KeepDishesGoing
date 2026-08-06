import { decodeJwt, isExpired, rolesOf, type JwtPayload } from "../infrastructure/jwt.ts";
import { load, remove, save } from "../infrastructure/storage.ts";

export type Role = "owner" | "driver" | "admin" | "user";

export type Session = {
	token: string;
	sub: string;
	username: string;
	roles: string[];
};

const STORAGE_KEY = "kdg.session";
let current: Session | null = restore();
const listeners = new Set<() => void>();

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
	};
}

export function getSession(): Session | null {
	return current;
}

export function hasRole(role: Role): boolean {
	return current !== null && current.roles.includes(role);
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
	notify();
}

export function onSessionChange(listener: () => void): () => void {
	listeners.add(listener);
	return () => listeners.delete(listener);
}

function notify(): void {
	for (const l of listeners) l();
}
