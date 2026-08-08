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
				"Open now",
			),
		];
		for (const [type, label] of Object.entries(RESTAURANT_TYPE_LABELS)) {
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
					label,
				),
			);
		}
		return h("div", { class: "filter-bar", role: "group", "aria-label": "Filters" }, ...chips);
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
