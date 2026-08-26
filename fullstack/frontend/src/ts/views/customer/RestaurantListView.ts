import { getPriceCategory, listRestaurants } from "../../api/restaurantApi.ts";
import {
	RESTAURANT_TYPE_LABELS,
	isOpen,
	type PriceCategory,
	type Restaurant,
	type RestaurantType,
} from "../../domain/Restaurant.ts";
import {
	emptyState,
	logoImg,
	skeletonCards,
	tag,
} from "../../presenter/components.ts";
import { h, mount } from "../../presenter/dom.ts";
import { money } from "../../presenter/format.ts";
import {
	distanceOriginCoordinates,
	distanceOriginLine,
	geocode,
	haversineKm,
} from "../../presenter/geo.ts";
import { allRestaurantsMap } from "../../presenter/map.ts";
import { saveGeneralLocation } from "../../state/location.ts";
import type { View } from "../View.ts";

type CardModel = Restaurant & { priceCategory: PriceCategory | null };
type SortKey = "recommended" | "distance" | "cost" | "name";

/** One entry in the quick-filter carousel. */
type QuickFilter = {
	key: string;
	label: string;
	icon: string;
	matchType: RestaurantType | null;
	nameMatch: RegExp | null;
};

const QUICK_FILTERS: QuickFilter[] = [
	{ key: "open", label: "Open now", icon: "clock.svg", matchType: null, nameMatch: null },
	{ key: "fastfood", label: "Fast food", icon: "burger.svg", matchType: "FAST_FOOD", nameMatch: null },
	{ key: "sandwich", label: "Sandwich bar", icon: "sandwich.svg", matchType: "BROODJESZAKEN", nameMatch: null },
	{ key: "comfort", label: "Comfort food", icon: "pizza.svg", matchType: "COMFORT_FOOD", nameMatch: null },
	{ key: "finedining", label: "Fine dining", icon: "finedining.svg", matchType: "FIJN_DINEREN", nameMatch: null },
	{ key: "seafood", label: "Seafood", icon: "seafood.svg", matchType: "VISRESTAURANTS", nameMatch: null },
	{ key: "michelin", label: "Michelin star", icon: "michelin.svg", matchType: "MICHELIN_STER", nameMatch: null },
	{ key: "chinese", label: "Chinese", icon: "noodles.svg", matchType: null, nameMatch: /chinese/i },
	{ key: "thai", label: "Thai", icon: "thai.svg", matchType: null, nameMatch: /thai/i },
	{ key: "sushi", label: "Sushi", icon: "sushi.svg", matchType: null, nameMatch: /sushi/i },
	{ key: "indian", label: "Indian", icon: "indian.svg", matchType: null, nameMatch: /indian/i },
	{ key: "mexican", label: "Mexican", icon: "taco.svg", matchType: null, nameMatch: /mexican|taco/i },
	{ key: "italian", label: "Italian", icon: "spaghetti.svg", matchType: null, nameMatch: /itali|pizza/i },
	{ key: "poke", label: "Poke bowls", icon: "pokebowl.svg", matchType: null, nameMatch: /poke/i },
];

/** Landscape banner per restaurant (cards only; menu header keeps the logo). */
const BANNERS: Record<string, string> = {
	"a6a52c73-9070-4128-a988-255383b941bc": "/img/banners/fries.jpg",
	"b7b62d84-1234-5678-9101-112131415161": "/img/banners/pizza.jpg",
	"c8c73e95-2345-6789-1011-121314151617": "/img/banners/sushi.jpg",
	"d9d84f06-3456-7891-0222-324252637485": "/img/banners/finedining.jpg",
	"e0ea1785-4567-8910-0333-435363748596": "/img/banners/kfc.jpg",
	"f1fa2896-5678-9012-0444-546474859607": "/img/banners/pizzahut.jpg",
	"a1ab3078-6789-0123-0555-657585960718": "/img/banners/dominos.jpg",
	"b2bc4189-7890-1234-0666-768696071829": "/img/banners/fiveguys.jpg",
	"c3cd5290-8901-2345-0777-8797a7182930": "/img/banners/pasta.jpg",
};

const DELIVERY_BASE_FEE = 2.99;
const DELIVERY_FEE_PER_KM = 0.49;

function deliveryFee(km: number): number {
	return Math.round((DELIVERY_BASE_FEE + DELIVERY_FEE_PER_KM * km) * 100) / 100;
}

/** US13 (open/closed), US39 (price category), browse + search + filter + sort restaurants. */
export class RestaurantListView implements View {
	private destroyed = false;
	private openOnly = false;
	private cuisineFilter: string | null = null;
	private search = "";
	private sort: SortKey = "recommended";
	private maxKm = 15;
	private origin: string | null = null;
	private cards: CardModel[] = [];
	private km = new Map<string, number>();
	private mapContainer: HTMLElement | null = null;

	async render(root: HTMLElement): Promise<void> {
		mount(root, h("div", { class: "view" }, header(), skeletonCards(6)));

		try {
			const restaurants = await listRestaurants();
			// Price category comes from the order-service strategy resolver (US39).
			this.cards = await Promise.all(
				restaurants.map(async (r) => ({
					...r,
					priceCategory: await getPriceCategory(r.id).catch(() => null),
				})),
			);
			if (this.destroyed) return;
			this.origin = distanceOriginLine();
			this.paint(root);
		} catch (error) {
			if (this.destroyed) return;
			mount(
				root,
				h(
					"div",
					{ class: "view" },
					header(),
					emptyState(
						"Could not load restaurants",
						error instanceof Error ? error.message : "Unknown error",
						h(
							"button",
							{ class: "btn btn-primary", onclick: () => void this.render(root) },
							"Retry",
						),
					),
				),
			);
		}
	}

	private filtered(): CardModel[] {
		const q = this.search.trim().toLowerCase();
		const cuisine = QUICK_FILTERS.find((f) => f.key === this.cuisineFilter) ?? null;
		return this.cards.filter((r) => {
			if (this.openOnly && !isOpen(r)) return false;
			if (cuisine?.matchType && r.restaurantType !== cuisine.matchType) return false;
			if (cuisine?.nameMatch) {
				const hay = `${r.name} ${r.fullAddress ?? ""}`;
				if (!cuisine.nameMatch.test(hay)) return false;
			}
			if (
				q !== "" &&
				!r.name.toLowerCase().includes(q) &&
				!(r.fullAddress ?? "").toLowerCase().includes(q)
			) {
				return false;
			}
			// Distance limit — only once that restaurant's km is known.
			if (this.origin && this.km.has(r.id) && (this.km.get(r.id) ?? 0) > this.maxKm) {
				return false;
			}
			return true;
		});
	}

	private sorted(list: CardModel[]): CardModel[] {
		const arr = [...list];
		switch (this.sort) {
			case "name":
				arr.sort((a, b) => a.name.localeCompare(b.name));
				break;
			case "cost":
				arr.sort(
					(a, b) =>
						(a.priceCategory?.length ?? 0) - (b.priceCategory?.length ?? 0) ||
						a.name.localeCompare(b.name),
				);
				break;
			case "distance":
				arr.sort(
					(a, b) =>
						(this.km.get(a.id) ?? Number.POSITIVE_INFINITY) -
							(this.km.get(b.id) ?? Number.POSITIVE_INFINITY) ||
						a.name.localeCompare(b.name),
				);
				break;
			default:
				arr.sort(
					(a, b) =>
						Number(isOpen(b)) - Number(isOpen(a)) || a.name.localeCompare(b.name),
				);
		}
		return arr;
	}

	private paint(root: HTMLElement): void {
		// If the map panel is open, keep its pins in sync with the current filters.
		if (this.mapContainer) void this.fillMap(this.mapContainer);

		// Keep the active chip in sync (carousel sits outside the grid) without
		// snapping the carousel back to the start.
		const wrap = document.getElementById("quick-filter-wrap");
		if (wrap && wrap.isConnected) {
			const inner = document.getElementById("quick-filter-bar");
			const scrollLeft = inner ? inner.scrollLeft : 0;
			wrap.replaceWith(this.filterBar(root));
			const nextInner = document.getElementById("quick-filter-bar");
			if (nextInner) nextInner.scrollLeft = scrollLeft;
		}

		// Remove the location prompt once a location is set.
		if (this.origin) {
			const loc = document.getElementById("location-bar");
			if (loc && loc.isConnected) loc.remove();
		}

		const visible = this.sorted(this.filtered());

		const grid = document.getElementById("restaurant-grid");
		if (grid) {
			if (visible.length === 0) {
				mount(
					grid,
					emptyState(
						"Nothing matches",
						this.cards.length === 0
							? "No restaurants yet - check back later."
							: "Try clearing the search and filters.",
						this.cards.length === 0
							? undefined
							: h("button", {
									class: "btn btn-secondary",
									onclick: () => {
										this.openOnly = false;
										this.cuisineFilter = null;
										this.search = "";
										const input = document.getElementById(
											"restaurant-search",
										) as HTMLInputElement | null;
										if (input) input.value = "";
										this.paint(root);
									},
								}, "Clear filters"),
					),
				);
			} else {
				mount(grid, ...visible.map((r) => this.card(r)));
			}
			void this.computeDistances(visible, root);
			return;
		}

		// Initial render - build full view with ids on the dynamic parts.
		mount(
			root,
			h(
				"div",
				{ class: "view" },
				header(),
				this.origin ? null : this.locationBar(root),
				this.exploreRow(root),
				this.filterBar(root),
				visible.length === 0
					? emptyState(
							"Nothing matches",
							this.cards.length === 0
								? "No restaurants yet - check back later."
								: "Try clearing the search and filters.",
							this.cards.length === 0
								? undefined
								: h("button", {
										class: "btn btn-secondary",
										onclick: () => {
											this.openOnly = false;
											this.cuisineFilter = null;
											this.search = "";
											const input = document.getElementById(
												"restaurant-search",
											) as HTMLInputElement | null;
											if (input) input.value = "";
											this.paint(root);
										},
									}, "Clear filters"),
						)
					: h(
							"div",
							{ id: "restaurant-grid", class: "grid-cards" },
							...visible.map((r) => this.card(r)),
						),
			),
		);
		void this.computeDistances(visible, root);
	}

	/** Search bar + sort select + "show all on map" button - one row, left to right. */
	private exploreRow(root: HTMLElement): HTMLElement {
		const searchInput = h("input", {
			id: "restaurant-search",
			class: "input",
			type: "search",
			placeholder: "Search restaurants, addresses…",
			"aria-label": "Search restaurants",
			oninput: (e: Event) => {
				this.search = (e.target as HTMLInputElement).value;
				this.paint(root);
			},
		});

		const sortSelect = h(
			"select",
			{
				class: "select",
				id: "restaurant-sort",
				"aria-label": "Sort restaurants",
				onchange: (e: Event) => {
					this.sort = (e.target as HTMLSelectElement).value as SortKey;
					this.paint(root);
				},
			},
			h("option", { value: "recommended" }, "Recommended"),
			h("option", { value: "distance", id: "sort-distance" }, "Distance"),
			h("option", { value: "cost" }, "Price: low to high"),
			h("option", { value: "name" }, "Name A–Z"),
		);

		// Distance sorting needs a location; reflect that in the option.
		void distanceOriginCoordinates().then((origin) => {
			if (this.destroyed) return;
			const opt = document.getElementById("sort-distance") as HTMLOptionElement | null;
			if (opt) opt.disabled = origin === null;
		});

		const distLabel = h("span", { class: "dist-label", id: "dist-label" }, `≤ ${this.maxKm} km`);
		const distanceControl = h(
			"div",
			{ class: "distance-control", title: "Maximum distance" },
			h("input", {
				type: "range",
				class: "distance-slider",
				min: "1",
				max: "30",
				value: String(this.maxKm),
				"aria-label": "Maximum distance",
				oninput: (e: Event) => {
					this.maxKm = Number((e.target as HTMLInputElement).value);
					const label = document.getElementById("dist-label");
					if (label) label.textContent = `≤ ${this.maxKm} km`;
					this.paint(root);
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
				onclick: () => this.openMapPanel(),
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

	/** Inline prompt for a general location when none is saved (guest / no address). */
	private locationBar(root: HTMLElement): HTMLElement {
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
					saveGeneralLocation(value);
					this.origin = value;
					this.paint(root);
				},
			},
			h("span", { class: "muted" }, "Set a location to see distances:"),
			input,
			saveBtn,
		);
	}

	/** Side panel map - stays open while you keep browsing the page. */
	private openMapPanel(): void {
		const content = h("div", { class: "map-all" });
		const close = (): void => {
			overlay.remove();
			this.mapContainer = null;
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
		this.mapContainer = content;
		content.textContent = "Loading map…";
		void this.fillMap(content);
	}

	private async fillMap(container: HTMLElement): Promise<void> {
		const home = await distanceOriginCoordinates();
		const visible = this.filtered();
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
		await allRestaurantsMap(points, home, container);
	}

	/** Async distance + delivery-fee estimate, using home address or general location. */
	private async computeDistances(list: CardModel[], root: HTMLElement): Promise<void> {
		const origin = await distanceOriginCoordinates();
		if (!origin || this.destroyed) return;
		let changed = false;
		for (const r of list) {
			if (this.destroyed) break;
			if (!r.fullAddress || this.km.has(r.id)) continue;
			const coord = await geocode(r.fullAddress);
			if (!coord) continue;
			const km = haversineKm(origin, coord);
			this.km.set(r.id, km);
			if (km > this.maxKm) changed = true;
		}
		// Re-render now that distances are known; card() shows cached km + fee,
		// and out-of-range restaurants are filtered out.
		if (changed && !this.destroyed) this.paint(root);
	}

	private filterBar(root: HTMLElement): HTMLElement {
		const chips = QUICK_FILTERS.map((f) => {
			const active = f.key === "open" ? this.openOnly : this.cuisineFilter === f.key;
			return h(
				"button",
				{
					class: `quick-filter ${active ? "active" : ""}`,
					"aria-pressed": String(active),
					onclick: () => {
						if (f.key === "open") {
							this.openOnly = !this.openOnly;
						} else {
							this.cuisineFilter = this.cuisineFilter === f.key ? null : f.key;
						}
						this.paint(root);
					},
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

	/** Uber Eats style card: full-width image, floating chips, info below. */
	private card(r: CardModel): HTMLElement {
		const open = isOpen(r);
		const banner = BANNERS[r.id] ?? r.logoUrl;
		return h(
			"a",
			{
				class: "card card-interactive restaurant-card",
				href: `#/restaurants/${r.id}`,
				"aria-label": `${r.name}, ${open ? "open" : "closed"}`,
			},
			h(
				"div",
				{ class: "restaurant-media" },
				logoImg(banner, r.name, "restaurant-banner"),
				r.priceCategory
					? h(
							"span",
							{ class: "price-chip", "aria-label": `Price category ${r.priceCategory}` },
							r.priceCategory,
						)
					: null,
				h(
					"span",
					{ class: `status-chip ${open ? "open" : "closed"}` },
					open ? "Open now" : "Closed",
				),
			),
			h(
				"div",
				{ class: "restaurant-body" },
				h(
					"div",
					{ class: "restaurant-name-row" },
					h("span", { class: "name" }, r.name),
					r.restaurantType ? tag(RESTAURANT_TYPE_LABELS[r.restaurantType]) : null,
				),
				r.fullAddress ? h("div", { class: "address" }, r.fullAddress) : null,
				h(
					"div",
					{ class: "meta-row" },
					r.openingHours ? h("span", { class: "meta" }, r.openingHours) : null,
					h(
						"span",
						{ class: "meta", id: `dist-${r.id}` },
						this.km.has(r.id) ? `${this.km.get(r.id)!.toFixed(1)} km` : "",
					),
					h(
						"span",
						{ class: "meta", id: `fee-${r.id}` },
						this.km.has(r.id) ? `delivery fee ~${money(deliveryFee(this.km.get(r.id)!))}` : "",
					),
				),
			),
		);
	}

	destroy(): void {
		this.destroyed = true;
	}
}

function header(): HTMLElement {
	return h(
		"div",
		{ class: "page-header" },
		h(
			"div",
			{},
			h("h1", {}, "Restaurants"),
			h("p", { class: "subtitle" }, "Pick a restaurant to see its menu"),
		),
	);
}
