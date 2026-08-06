import { h } from "./dom.ts";

export interface AddressSuggestion {
	displayName: string;
	street: string;
	houseNumber: string;
	postalCode: string;
	city: string;
	country: string;
}

/**
 * Nominatim-powered address autocomplete. Replaces a plain address input
 * with a debounced search + suggestions dropdown. Falls back gracefully to
 * a plain input when Nominatim is unreachable (offline / rate-limited).
 *
 * @param container - the parent element that owns the input
 * @param input - the text input element to enhance (should already have class="input")
 * @param onSelect - called when the user picks a suggestion
 */
export function addressAutocomplete(
	container: HTMLElement,
	input: HTMLInputElement,
	onSelect: (parsed: AddressSuggestion) => void,
): void {
	if (!container || !input) return;

	let timer: number | undefined;
	let dropdown: HTMLElement | null = null;
	let abort: AbortController | null = null;
	let destroyed = false;

	function removeDropdown(): void {
		if (dropdown) {
			dropdown.remove();
			dropdown = null;
		}
	}

	async function search(query: string): Promise<void> {
		removeDropdown();
		abort?.abort();
		if (query.trim().length < 3) return;

		const ctrl = new AbortController();
		abort = ctrl;

		try {
			const url = new URL("https://nominatim.openstreetmap.org/search");
			url.searchParams.set("q", query);
			url.searchParams.set("format", "json");
			url.searchParams.set("addressdetails", "1");
			url.searchParams.set("limit", "5");
			url.searchParams.set("countrycodes", "be,nl");

			const res = await fetch(url, {
				signal: ctrl.signal,
				headers: { "Accept-Language": "en" },
			});
			if (!res.ok || destroyed) return;

			const data: NominatimResult[] = await res.json();
			if (data.length === 0 || destroyed) return;

			const suggestions: AddressSuggestion[] = data.map(parseSuggestion).filter(Boolean) as AddressSuggestion[];
			if (suggestions.length === 0) return;

			dropdown = showDropdown(input, suggestions, onSelect);
		} catch {
			/* offline / rate-limited - graceful degradation */
		}
	}

	input.addEventListener("input", () => {
		window.clearTimeout(timer);
		removeDropdown();
		timer = window.setTimeout(() => void search(input.value), 300);
	});

	input.addEventListener("blur", () => {
		setTimeout(removeDropdown, 200);
	});

	input.addEventListener("focus", () => {
		if (input.value.trim().length >= 3) void search(input.value);
	});

	const observer = new MutationObserver(() => {
		if (!document.body.contains(input)) {
			destroyed = true;
			removeDropdown();
			observer.disconnect();
		}
	});
	observer.observe(container, { childList: true, subtree: true });
}

function showDropdown(
	input: HTMLInputElement,
	suggestions: AddressSuggestion[],
	onSelect: (parsed: AddressSuggestion) => void,
): HTMLElement {
	const rect = input.getBoundingClientRect();
	const list = h(
		"ul",
		{
			class: "autocomplete-dropdown",
			style: `position:fixed;top:${rect.bottom}px;left:${rect.left}px;width:${rect.width}px;z-index:var(--z-dropdown,100);background:var(--surface,#fff);border:1px solid var(--border,#e2e8f0);border-radius:var(--radius-md,8px);box-shadow:var(--shadow-md,0 4px 12px rgba(15,23,42,.08));max-height:240px;overflow-y:auto;list-style:none;padding:var(--space-1,4px) 0;margin:0`,
		},
		...suggestions.map((s) =>
			h(
				"li",
				{
					style: "padding:var(--space-2,8px) var(--space-3,12px);cursor:pointer;font-size:var(--text-sm,14px)",
					onmouseenter: (e: Event) => {
						(e.target as HTMLElement).style.background = "var(--accent-soft,#fff7ed)";
					},
					onmouseleave: (e: Event) => {
						(e.target as HTMLElement).style.background = "";
					},
					onclick: () => {
						input.value = s.displayName;
						onSelect(s);
						list.remove();
					},
				},
				h("span", { style: "font-weight:600;display:block" }, s.displayName),
				h(
					"span",
					{ style: "color:var(--text-muted,#475569);font-size:var(--text-xs,12px)" },
					[s.postalCode, s.city, s.country].filter(Boolean).join(", "),
				),
			),
		),
	);
	document.body.appendChild(list);

	// Close dropdown on outside click
	const outsideClick = (e: MouseEvent) => {
		if (!list.contains(e.target as Node)) { list.remove(); document.removeEventListener("click", outsideClick); }
	};
	setTimeout(() => document.addEventListener("click", outsideClick), 0);

	return list;
}

function parseSuggestion(r: NominatimResult): AddressSuggestion | null {
	const a = r.address;
	if (!a) return null;

	const street = a.road ?? a.pedestrian ?? a.footway ?? a.path ?? a.street_name ?? "";
	const houseNumber = a.house_number ?? "";
	const postalCode = a.postcode ?? "";
	const city = a.city ?? a.town ?? a.village ?? a.municipality ?? "";
	const country = a.country ?? "Belgium";

	if (!street && !city) return null;

	const displayParts: string[] = [];
	if (street) displayParts.push(street + (houseNumber ? ` ${houseNumber}` : ""));
	if (postalCode || city) displayParts.push([postalCode, city].filter(Boolean).join(" "));
	if (country) displayParts.push(country);

	return {
		displayName: displayParts.join(", "),
		street,
		houseNumber,
		postalCode,
		city,
		country,
	};
}

interface NominatimResult {
	address?: {
		road?: string;
		pedestrian?: string;
		footway?: string;
		path?: string;
		street_name?: string;
		house_number?: string;
		postcode?: string;
		city?: string;
		town?: string;
		village?: string;
		municipality?: string;
		country?: string;
	};
}
