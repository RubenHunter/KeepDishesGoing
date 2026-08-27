import { ensureSession, getSession } from "../state/session.ts";
import { resetUserState } from "../state/userState.ts";

export class ApiError extends Error {
	readonly status: number;

	constructor(status: number, message: string) {
		super(message);
		this.name = "ApiError";
		this.status = status;
	}
}

type Options = {
	method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
	body?: unknown;
	auth?: boolean;
};

/** JSON fetch wrapper. Injects Bearer token when auth=true. Throws ApiError on !ok. */
export async function request<T>(url: string, options: Options = {}): Promise<T> {
	const response = await send(url, options);
	if (response.status === 204) return undefined as T;
	const text = await response.text();
	if (text.length === 0) return undefined as T; // 200/201 with empty body
	return JSON.parse(text) as T;
}

/** Like request, but returns the raw Response (for Location headers on 201). */
export async function requestRaw(url: string, options: Options = {}): Promise<Response> {
	return send(url, options);
}

async function send(url: string, options: Options): Promise<Response> {
	const { method = "GET", body, auth = false } = options;
	const headers: Record<string, string> = {};
	if (body !== undefined) headers["Content-Type"] = "application/json";
	if (auth) {
		const session = await ensureSession();
		if (session) headers["Authorization"] = `Bearer ${session.token}`;
	}

	let response: Response;
	try {
		response = await fetch(url, {
			method,
			headers,
			body: body === undefined ? undefined : JSON.stringify(body),
		});
	} catch {
		throw new ApiError(0, "Service unreachable. Is the backend running?");
	}

	if (response.status === 401) {
		// Token expired/invalid - session is gone, force re-login.
		if (auth) resetUserState();
		throw new ApiError(response.status, "Session expired. Please log in again.");
	}

	if (response.status === 403) {
		// Forbidden for this action - session itself is still valid. Keep it.
		throw new ApiError(response.status, "You are not allowed to do that.");
	}

	if (!response.ok) {
		throw new ApiError(response.status, await extractMessage(response));
	}
	return response;
}

/** Binary download (PDF report - US38). Returns object URL; caller revokes. */
export async function download(url: string, filename: string): Promise<void> {
	const session = getSession();
	const response = await fetch(url, {
		headers: session ? { Authorization: `Bearer ${session.token}` } : {},
	});
	if (!response.ok) throw new ApiError(response.status, await extractMessage(response));
	const blob = await response.blob();
	const objectUrl = URL.createObjectURL(blob);
	const anchor = document.createElement("a");
	anchor.href = objectUrl;
	anchor.download = filename;
	anchor.click();
	URL.revokeObjectURL(objectUrl);
}

async function extractMessage(response: Response): Promise<string> {
	try {
		const data: unknown = await response.json();
		if (typeof data === "object" && data !== null) {
			const d = data as Record<string, unknown>;
			if (typeof d.message === "string") return d.message;
			if (typeof d.detail === "string") return d.detail;
			if (typeof d.error === "string") return d.error;
		}
	} catch {
		/* non-JSON body */
	}
	return `Request failed (${response.status})`;
}
