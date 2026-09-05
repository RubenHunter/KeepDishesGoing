/** localStorage wrapper - JSON-safe, silent on quota/parse errors. */

export function load<T>(key: string): T | null {
	try {
		const raw = localStorage.getItem(key);
		return raw === null ? null : (JSON.parse(raw) as T);
	} catch {
		return null;
	}
}

export function save(key: string, value: unknown): void {
	try {
		localStorage.setItem(key, JSON.stringify(value));
	} catch {
		/* quota exceeded - non-fatal */
	}
}

export function remove(key: string): void {
	localStorage.removeItem(key);
}
