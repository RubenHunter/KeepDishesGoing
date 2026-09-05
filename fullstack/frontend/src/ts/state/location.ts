import { load, save } from "../infrastructure/storage.ts";
import { getSession } from "./session.ts";

/**
 * Lightweight "general location" (postcode / city) used for distance
 * features when no full home address is saved. Scoped per Keycloak subject.
 */
function key(): string {
	return `kdg.location.${getSession()?.sub ?? "guest"}`;
}

export function savedGeneralLocation(): string | null {
	return load<string>(key());
}

export function saveGeneralLocation(line: string): void {
	save(key(), line.trim());
}
