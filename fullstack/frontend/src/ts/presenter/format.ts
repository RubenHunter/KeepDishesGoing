import type { Eur } from "../domain/types.ts";

const moneyFormat = new Intl.NumberFormat("nl-BE", {
	style: "currency",
	currency: "EUR",
});

export function money(amount: Eur): string {
	return moneyFormat.format(amount);
}

/** Java LocalDateTime ("2026-07-27T13:45:00") parses as local time - correct for display. */
export function dateTime(iso: string): string {
	return new Date(iso).toLocaleString("nl-BE", {
		day: "2-digit",
		month: "2-digit",
		hour: "2-digit",
		minute: "2-digit",
	});
}

export function timeOnly(iso: string): string {
	return new Date(iso).toLocaleTimeString("nl-BE", {
		hour: "2-digit",
		minute: "2-digit",
	});
}

export function countdown(deadlineIso: string, nowMs: number): string {
	const remaining = Math.max(0, new Date(deadlineIso).getTime() - nowMs);
	const minutes = Math.floor(remaining / 60000);
	const seconds = Math.floor((remaining % 60000) / 1000);
	return `${minutes}:${String(seconds).padStart(2, "0")}`;
}
