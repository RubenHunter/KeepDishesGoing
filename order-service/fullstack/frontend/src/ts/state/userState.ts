import { clearSession } from "./session.ts";
import { forgetLocalCart } from "./cart.ts";

/**
 * Resets browser state on explicit logout or 401 session expiry.
 * Profile, order history and tracked order stay in per-account storage
 * (scoped by Keycloak subject) so they survive logout/login.
 */
export function resetUserState(): void {
	clearSession();
	forgetLocalCart();
}
