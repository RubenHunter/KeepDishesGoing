import { KEYCLOAK, tokenEndpoint } from "../config.ts";
import { setTokens, type Session } from "../state/session.ts";

type TokenResponse = {
	access_token: string;
	refresh_token?: string;
	expires_in: number;
};

/**
 * Direct access grant against Keycloak (demo client has direct grants ON).
 * Throws Error with a displayable message on failure.
 */
export async function login(username: string, password: string): Promise<Session> {
	const body = new URLSearchParams({
		grant_type: "password",
		client_id: KEYCLOAK.clientId,
		username,
		password,
	});
	if (KEYCLOAK.clientSecret) body.set("client_secret", KEYCLOAK.clientSecret);

	let response: Response;
	try {
		response = await fetch(tokenEndpoint(), {
			method: "POST",
			headers: { "Content-Type": "application/x-www-form-urlencoded" },
			body,
		});
	} catch {
		throw new Error("Keycloak unreachable. Is it running on port 8180?");
	}

	if (!response.ok) {
		throw new Error(
			response.status === 401
				? "Invalid username or password"
				: `Login failed (${response.status})`,
		);
	}

	const data = (await response.json()) as TokenResponse;
	return setTokens(data.access_token, data.refresh_token);
}
