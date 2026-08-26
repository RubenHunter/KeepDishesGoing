import { getPriceCategory, listRestaurants } from "../../api/restaurantApi.ts";
import { emptyState, skeletonCards } from "../../presenter/components.ts";
import { h, mount } from "../../presenter/dom.ts";
import { distanceOriginLine } from "../../presenter/geo.ts";
import { saveGeneralLocation } from "../../state/location.ts";
import type { View } from "../View.ts";
import { computeDistances } from "./restaurantList/distance.ts";
import { exploreBar } from "./restaurantList/exploreBar.ts";
import { filterBar } from "./restaurantList/filterBar.ts";
import {
	applyFilters,
	sortCards,
	type CardModel,
	type FilterState,
	type SortKey,
} from "./restaurantList/filters.ts";
import { locationBar } from "./restaurantList/locationBar.ts";
import { openMapPanel, type MapPanel } from "./restaurantList/mapPanel.ts";
import { restaurantCard } from "./restaurantList/restaurantCard.ts";

/**
 * US13 (open/closed), US39 (price category), browse + search + filter + sort
 * restaurants. Thin collection page - filter/search/sort live in filters.ts,
 * distance + map live in distance.ts / mapPanel.ts.
 */
export class RestaurantListView implements View {
	private destroyed = false;
	private root: HTMLElement | null = null;
	private openOnly = false;
	private cuisineFilter: string | null = null;
	private search = "";
	private sort: SortKey = "recommended";
	private maxKm = 15;
	private origin: string | null = null;
	private cards: CardModel[] = [];
	private km = new Map<string, number>();
	private map: MapPanel | null = null;

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

	private state(): FilterState {
		return {
			openOnly: this.openOnly,
			cuisine: this.cuisineFilter,
			search: this.search,
			sort: this.sort,
			maxKm: this.maxKm,
			origin: this.origin,
		};
	}

	private filtered(): CardModel[] {
		return applyFilters(this.cards, this.state(), this.km);
	}

	private sorted(list: CardModel[]): CardModel[] {
		return sortCards(list, this.sort, this.km);
	}

	private paint(root: HTMLElement): void {
		this.root = root;

		// Keep the open map's pins in sync with the current filters.
		if (this.map?.isOpen()) void this.map.fill(this.filtered());

		// Refresh the carousel without snapping it back to the start.
		const wrap = document.getElementById("quick-filter-wrap");
		if (wrap && wrap.isConnected) {
			const inner = document.getElementById("quick-filter-bar");
			const scrollLeft = inner ? inner.scrollLeft : 0;
			wrap.replaceWith(filterBar(this.openOnly, this.cuisineFilter, (key) => this.toggleFilter(key)));
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
				mount(grid, this.emptyMessage());
			} else {
				mount(grid, ...visible.map((r) => restaurantCard(r, this.km)));
			}
			void this.refreshDistances(visible);
			return;
		}

		// Initial render - build full view with ids on the dynamic parts.
		mount(
			root,
			h(
				"div",
				{ class: "view" },
				header(),
				this.origin
					? null
					: locationBar((value) => {
							saveGeneralLocation(value);
							this.origin = value;
							this.paint(root);
						}),
				exploreBar({
					maxKm: this.maxKm,
					onSearch: (value) => {
						this.search = value;
						this.paint(root);
					},
					onSort: (sort) => {
						this.sort = sort;
						this.paint(root);
					},
					onMaxKm: (value) => {
						this.maxKm = value;
						this.paint(root);
					},
					onMap: () => this.openMap(),
					isDestroyed: () => this.destroyed,
				}),
				filterBar(this.openOnly, this.cuisineFilter, (key) => this.toggleFilter(key)),
				visible.length === 0
					? this.emptyMessage()
					: h(
							"div",
							{ id: "restaurant-grid", class: "grid-cards" },
							...visible.map((r) => restaurantCard(r, this.km)),
						),
			),
		);
		void this.refreshDistances(visible);
	}

	private toggleFilter(key: string): void {
		if (key === "open") {
			this.openOnly = !this.openOnly;
		} else {
			this.cuisineFilter = this.cuisineFilter === key ? null : key;
		}
		if (this.root) this.paint(this.root);
	}

	private emptyMessage(): HTMLElement {
		const none = this.cards.length === 0;
		return emptyState(
			"Nothing matches",
			none ? "No restaurants yet - check back later." : "Try clearing the search and filters.",
			none
				? undefined
				: h(
						"button",
						{
							class: "btn btn-secondary",
							onclick: () => {
								this.openOnly = false;
								this.cuisineFilter = null;
								this.search = "";
								const input = document.getElementById(
									"restaurant-search",
								) as HTMLInputElement | null;
								if (input) input.value = "";
								if (this.root) this.paint(this.root);
							},
						},
						"Clear filters",
					),
		);
	}

	private openMap(): void {
		this.map = openMapPanel();
		void this.map.fill(this.filtered());
	}

	private async refreshDistances(visible: CardModel[]): Promise<void> {
		const changed = await computeDistances(visible, this.km, this.maxKm, () => this.destroyed);
		if (changed && !this.destroyed && this.root) this.paint(this.root);
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
