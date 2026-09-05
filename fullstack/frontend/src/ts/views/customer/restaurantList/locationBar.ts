import { h } from "../../../presenter/dom.ts";

/** Inline prompt for a general location when none is saved (guest / no address). */
export function locationBar(onSave: (value: string) => void): HTMLElement {
	const input = h("input", {
		class: "input",
		placeholder: "Postcode or city (for distance)",
		"aria-label": "Your postcode or city",
	});
	const saveBtn = h(
		"button",
		{ class: "btn btn-primary btn-sm", type: "submit" },
		"Save location",
	);
	return h(
		"form",
		{
			class: "location-bar",
			id: "location-bar",
			onsubmit: (e: Event) => {
				e.preventDefault();
				const value = input.value.trim();
				if (!value) return;
				onSave(value);
			},
		},
		h("span", { class: "muted" }, "Set a location to see distances:"),
		input,
		saveBtn,
	);
}
