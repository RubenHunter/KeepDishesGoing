import { h } from "../../../presenter/dom.ts";
import { QUICK_FILTERS } from "./filters.ts";

/**
 * Horizontally scrollable quick-filter carousel.
 * `onToggle` receives the filter key; "open" toggles the open-only flag,
 * any other key toggles the cuisine filter.
 */
export function filterBar(
	openOnly: boolean,
	cuisine: string | null,
	onToggle: (key: string) => void,
): HTMLElement {
	const chips = QUICK_FILTERS.map((f) => {
		const active = f.key === "open" ? openOnly : cuisine === f.key;
		return h(
			"button",
			{
				class: `quick-filter ${active ? "active" : ""}`,
				"aria-pressed": String(active),
				onclick: () => onToggle(f.key),
			},
			h("img", {
				src: `/quick-filters/${f.icon}`,
				alt: "",
				loading: "lazy",
				"aria-hidden": "true",
			}),
			h("span", {}, f.label),
		);
	});

	const arrow = (dir: 1 | -1, label: string, icon: string): HTMLElement =>
		h(
			"button",
			{
				class: "quick-filter-arrow",
				"aria-label": label,
				onclick: () => {
					const bar = document.getElementById("quick-filter-bar");
					if (bar) bar.scrollBy({ left: dir * bar.clientWidth * 0.85, behavior: "smooth" });
				},
			},
			h("img", { src: `/quick-filters/${icon}`, alt: "", "aria-hidden": "true" }),
		);

	return h(
		"div",
		{ class: "quick-filter-wrap", id: "quick-filter-wrap" },
		arrow(-1, "Scroll filters left", "chevron-left.svg"),
		h(
			"div",
			{ class: "quick-filter-bar", id: "quick-filter-bar", role: "group", "aria-label": "Quick filters" },
			...chips,
		),
		arrow(1, "Scroll filters right", "chevron-right.svg"),
	);
}
