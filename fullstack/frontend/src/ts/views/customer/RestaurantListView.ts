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
	openBadge,
	skeletonCards,
	tag,
} from "../../presenter/components.ts";
import { h, mount } from "../../presenter/dom.ts";
import type { View } from "../View.ts";

type CardModel = Restaurant & { priceCategory: PriceCategory | null };

/** Quick-filter image per cuisine (takeaway.com-style horizontal carousel). */
const TYPE_IMAGES: Record<RestaurantType, string> = {
	FAST_FOOD: "/img/dishes/fg-cheeseburger.jpg",
	BROODJESZAKEN: "https://cdn.pixabay.com/photo/2017/01/09/13/21/tomato-1966418_1280.jpg",
	COMFORT_FOOD: "/img/dishes/ph-margherita.jpg",
	FIJN_DINEREN: "https://cdn.pixabay.com/photo/2017/06/23/00/44/pasta-2433027_1280.jpg",
	VISRESTAURANTS: "https://cdn.pixabay.com/photo/2016/11/23/18/31/sushi-1854032_1280.jpg",
	MICHELIN_STER: "https://cdn.pixabay.com/photo/2019/07/28/09/16/steak-4368254_1280.jpg",
};

/** US13 (open/closed), US39 (price category), browse + filter restaurants. */
export class RestaurantListView implements View {
	private destroyed = false;
	private openOnly = false;
	private typeFilter: RestaurantType | null = null;
	private cards: CardModel[] = [];

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

	private paint(root: HTMLElement): void {
		// Re-render the filter bar so the active chip is always visible.
		const bar = document.getElementById("filter-bar");
		if (bar && bar.isConnected) bar.replaceWith(this.filterBar(root));

		const visible = this.cards.filter(
			(r) =>
				(!this.openOnly || isOpen(r)) &&
				(this.typeFilter === null || r.restaurantType === this.typeFilter),
		);

		const grid = document.getElementById("restaurant-grid");
		if (grid) {
			if (visible.length === 0) {
				mount(grid,
					emptyState(
						"Nothing matches",
						this.cards.length === 0 ? "No restaurants yet - check back later." : "Try clearing the filters.",
						this.cards.length === 0 ? undefined
							: h("button", { class: "btn btn-secondary", onclick: () => {
									this.openOnly = false; this.typeFilter = null; this.paint(root);
								} }, "Clear filters"),
					));
			} else {
				mount(grid, ...visible.map((r) => this.card(r)));
			}
			return;
		}

		// Initial render - build full view with id on grid
		mount(
			root,
			h(
				"div",
				{ class: "view" },
				header(),
				this.filterBar(root),
				visible.length === 0
					? emptyState(
							"Nothing matches",
							this.cards.length === 0
								? "No restaurants yet - check back later."
								: "Try clearing the filters.",
							this.cards.length === 0
								? undefined
								: h(
										"button",
										{
											class: "btn btn-secondary",
											onclick: () => {
												this.openOnly = false;
												this.typeFilter = null;
												this.paint(root);
											},
										},
										"Clear filters",
									),
						)
					: h("div", { id: "restaurant-grid", class: "grid-cards" }, ...visible.map((r) => this.card(r))),
			),
		);
	}

	private filterBar(root: HTMLElement): HTMLElement {
		const chips: HTMLElement[] = [
			h(
				"button",
				{
					class: `filter-chip ${this.openOnly ? "active" : ""}`,
					"aria-pressed": String(this.openOnly),
					onclick: () => {
						this.openOnly = !this.openOnly;
						this.paint(root);
					},
				},
				h("span", {}, "Open now"),
			),
		];
		for (const [type, label] of Object.entries(RESTAURANT_TYPE_LABELS)) {
			const image = TYPE_IMAGES[type as RestaurantType];
			chips.push(
				h(
					"button",
					{
						class: `filter-chip ${this.typeFilter === type ? "active" : ""}`,
						"aria-pressed": String(this.typeFilter === type),
						onclick: () => {
							this.typeFilter =
								this.typeFilter === type ? null : (type as RestaurantType);
							this.paint(root);
						},
					},
					h(
						"img",
						{
							class: "filter-chip-img",
							src: image,
							alt: "",
							loading: "lazy",
							"aria-hidden": "true",
						},
					),
					h("span", {}, label),
				),
			);
		}
		return h(
			"div",
			{ class: "filter-bar", id: "filter-bar", role: "group", "aria-label": "Filters" },
			...chips,
		);
	}

	private card(r: CardModel): HTMLElement {
		const open = isOpen(r);
		return h(
			"a",
			{
				class: "card card-interactive restaurant-card",
				href: `#/restaurants/${r.id}`,
				"aria-label": `${r.name}, ${open ? "open" : "closed"}`,
			},
			logoImg(r.logoUrl, r.name, "restaurant-logo"),
			h(
				"div",
				{ class: "info" },
				h("div", { class: "name" }, r.name, openBadge(open)),
				r.fullAddress ? h("div", { class: "meta" }, h("span", { class: "muted" }, r.fullAddress)) : null,
				h(
					"div",
					{ class: "tags" },
					r.restaurantType ? tag(RESTAURANT_TYPE_LABELS[r.restaurantType]) : null,
					r.priceCategory
						? h(
								"span",
								{ class: "price-category", "aria-label": `Price category ${r.priceCategory}` },
								r.priceCategory,
							)
						: null,
					r.openingHours ? tag(r.openingHours) : null,
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
