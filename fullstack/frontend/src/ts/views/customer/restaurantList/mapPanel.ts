import { h } from "../../../presenter/dom.ts";
import { distanceOriginCoordinates, geocode } from "../../../presenter/geo.ts";
import { allRestaurantsMap } from "../../../presenter/map.ts";
import type { CardModel } from "./filters.ts";

export type MapPanel = {
	isOpen: () => boolean;
	close: () => void;
	fill: (visible: CardModel[]) => Promise<void>;
};

/** Side panel map - stays open while you keep browsing the page. */
export function openMapPanel(): MapPanel {
	const content = h("div", { class: "map-all" });
	let open = false;

	const close = (): void => {
		if (!open) return;
		open = false;
		overlay.remove();
		document.body.classList.remove("map-open");
		document.removeEventListener("keydown", onKey);
		window.removeEventListener("hashchange", close);
	};
	const onKey = (e: KeyboardEvent): void => {
		if (e.key === "Escape") close();
	};
	const closeBtn = h(
		"button",
		{ class: "icon-btn", "aria-label": "Close map", title: "Close map", onclick: close },
		h("img", { src: "/quick-filters/x.svg", alt: "", "aria-hidden": "true" }),
	);
	const overlay = h(
		"div",
		{ class: "map-panel-overlay" },
		h(
			"aside",
			{ class: "map-panel", role: "dialog", "aria-label": "Restaurants on the map" },
			h(
				"div",
				{ class: "map-panel-header" },
				h("h2", {}, "Restaurants on the map"),
				closeBtn,
			),
			content,
		),
	);

	// Push the page left only when there's room, so the panel doesn't cover content.
	const PANEL_WIDTH = 480;
	const MIN_CONTENT = 760;
	if (window.innerWidth - PANEL_WIDTH >= MIN_CONTENT) {
		document.body.classList.add("map-open");
	}
	document.addEventListener("keydown", onKey);
	// Close when the user navigates (e.g. opens a restaurant) — keeps it out of the way.
	window.addEventListener("hashchange", close);
	document.body.append(overlay);
	open = true;
	content.textContent = "Loading map…";

	async function fill(visible: CardModel[]): Promise<void> {
		if (!open) return;
		const home = await distanceOriginCoordinates();
		const points = (
			await Promise.all(
				visible.map(async (r) => {
					if (!r.fullAddress) return null;
					const coord = await geocode(r.fullAddress);
					return coord
						? { name: r.name, lat: coord.lat, lon: coord.lon, href: `#/restaurants/${r.id}` }
						: null;
				}),
			)
		).filter((p): p is { name: string; lat: number; lon: number; href: string } => p !== null);
		await allRestaurantsMap(points, home, content);
	}

	return { isOpen: () => open, close, fill };
}
