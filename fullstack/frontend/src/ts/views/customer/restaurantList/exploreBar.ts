import { distanceOriginCoordinates } from "../../../presenter/geo.ts";
import { h } from "../../../presenter/dom.ts";
import type { SortKey } from "./filters.ts";

type ExploreBarOptions = {
	maxKm: number;
	onSearch: (value: string) => void;
	onSort: (sort: SortKey) => void;
	onMaxKm: (value: number) => void;
	onMap: () => void;
	isDestroyed: () => boolean;
};

/** Search bar + sort select + distance slider + "show all on map" button. */
export function exploreBar(opts: ExploreBarOptions): HTMLElement {
	const searchInput = h("input", {
		id: "restaurant-search",
		class: "input",
		type: "search",
		placeholder: "Search restaurants, addresses…",
		"aria-label": "Search restaurants",
		oninput: (e: Event) => opts.onSearch((e.target as HTMLInputElement).value),
	});

	const sortSelect = h(
		"select",
		{
			class: "select",
			id: "restaurant-sort",
			"aria-label": "Sort restaurants",
			onchange: (e: Event) => opts.onSort((e.target as HTMLSelectElement).value as SortKey),
		},
		h("option", { value: "recommended" }, "Recommended"),
		h("option", { value: "distance", id: "sort-distance" }, "Distance"),
		h("option", { value: "cost" }, "Price: low to high"),
		h("option", { value: "name" }, "Name A–Z"),
	);

	// Distance sorting needs a location; reflect that in the option.
	void distanceOriginCoordinates().then((origin) => {
		if (opts.isDestroyed()) return;
		const opt = document.getElementById("sort-distance") as HTMLOptionElement | null;
		if (opt) opt.disabled = origin === null;
	});

	const distLabel = h("span", { class: "dist-label", id: "dist-label" }, `≤ ${opts.maxKm} km`);
	const distanceControl = h(
		"div",
		{ class: "distance-control", title: "Maximum distance" },
		h("input", {
			type: "range",
			class: "distance-slider",
			min: "1",
			max: "30",
			value: String(opts.maxKm),
			"aria-label": "Maximum distance",
			oninput: (e: Event) => {
				const value = Number((e.target as HTMLInputElement).value);
				const label = document.getElementById("dist-label");
				if (label) label.textContent = `≤ ${value} km`;
				opts.onMaxKm(value);
			},
		}),
		distLabel,
	);

	const mapBtn = h(
		"button",
		{
			class: "icon-btn",
			"aria-label": "Show restaurants on a map",
			title: "Show restaurants on a map",
			onclick: () => opts.onMap(),
		},
		h("img", {
			src: "/quick-filters/map-pin.svg",
			alt: "",
			"aria-hidden": "true",
		}),
	);

	return h(
		"div",
		{ class: "explore-row" },
		h("div", { class: "explore-search" }, searchInput),
		sortSelect,
		distanceControl,
		mapBtn,
	);
}
