import {
	getMenu,
	getPriceCategory,
	getRestaurantDetail,
	getRestaurantStatus,
} from "../../api/restaurantApi.ts";
import type { Dish, DishCategory } from "../../domain/Dish.ts";
import { RESTAURANT_TYPE_LABELS, type RestaurantDetail } from "../../domain/Restaurant.ts";
import {
	breadcrumb,
	emptyState,
	logoImg,
	openBadge,
	skeletonLines,
	tag,
} from "../../presenter/components.ts";
import { h, mount } from "../../presenter/dom.ts";
import { restaurantMap } from "../../presenter/map.ts";
import { ensureCart, onCartChange } from "../../state/cart.ts";
import type { View } from "../View.ts";
import { addToCart, cartPanel } from "./restaurantMenu/cartPanel.ts";
import { dishCard as renderDishCard } from "./restaurantMenu/dishCard.ts";
import { filterMenu, menuFilterBar, type MenuSort } from "./restaurantMenu/menuFilters.ts";

/**
 * US15 (one restaurant at a time), US16 (single-restaurant cart), dish states.
 * Thin page - dish cards / cart panel / filter bar live in restaurantMenu/.
 */
export class RestaurantMenuView implements View {
	private destroyed = false;
	private root: HTMLElement | null = null;
	private unsubscribe: (() => void) | null = null;
	private restaurant: RestaurantDetail | null = null;
	private priceCategory: string | null = null;
	private filterCategory: DishCategory | null = null;
	private menuData: Dish[] = [];
	private sortOrder: MenuSort = "none";

	async render(root: HTMLElement, params: Record<string, string>): Promise<void> {
		const restaurantId = params.id;
		mount(root, h("div", { class: "view" }, skeletonLines(6)));

		try {
			const [restaurant, menu, category, status] = await Promise.all([
				getRestaurantDetail(restaurantId),
				getMenu(restaurantId),
				getPriceCategory(restaurantId).catch(() => null),
				getRestaurantStatus(restaurantId).catch(() => null),
			]);
			if (this.destroyed) return;
			// Open flag: /status is authoritative (proxy boolean is unreliable).
			this.restaurant = { ...restaurant, open: status?.open ?? restaurant.open };
			this.priceCategory = category;
			this.menuData = menu;
			// Cart needs a session — a guest browsing without login still gets the full menu;
			// cart init failures only disable add-to-cart, never the whole page.
			await ensureCart().catch(() => null);
			this.unsubscribe = onCartChange(() => {
				if (!this.destroyed) this.paint(root);
			});
			this.paint(root);
		} catch (error) {
			if (this.destroyed) return;
			mount(
				root,
				h(
					"div",
					{ class: "view" },
					emptyState(
						"Could not load this restaurant",
						error instanceof Error ? error.message : "Unknown error",
						h("a", { class: "btn btn-primary", href: "#/" }, "Back to restaurants"),
					),
				),
			);
		}
	}

	private paint(root: HTMLElement): void {
		this.root = root;
		const restaurant = this.restaurant;
		if (!restaurant) return;

		const filtered = filterMenu(this.menuData, this.filterCategory, this.sortOrder);

		// Already painted - only update the dish list, filters and cart panel.
		const existingStack = document.getElementById("menu-dish-stack");
		const existingFilter = document.getElementById("menu-filter-bar");
		if (existingStack && existingFilter) {
			mount(existingFilter, this.filterBar());
			if (filtered.length === 0) {
				mount(existingStack, emptyState("No dishes match", "Try clearing the filters."));
			} else {
				mount(
					existingStack,
					h("div", { class: "stack" }, ...filtered.map((d) => renderDishCard(restaurant, d, (dish) => void addToCart(restaurant, dish)))),
				);
			}
			const cartSlot = document.getElementById("menu-cart-panel");
			if (cartSlot) mount(cartSlot, cartPanel(restaurant.id));
			return;
		}

		// First render.
		const mapSlot = h("div");
		if (restaurant.fullAddress) {
			void restaurantMap(restaurant.fullAddress, mapSlot);
		}

		mount(
			root,
			h(
				"div",
				{ class: "view" },
				breadcrumb([
					{ label: "Restaurants", href: "#/" },
					{ label: restaurant.name },
				]),
				h(
					"div",
					{ class: "page-header" },
					h(
						"div",
						{},
						h(
							"div",
							{ class: "cluster" },
							restaurant.logoUrl
								? (() => {
										const el = logoImg(restaurant.logoUrl, restaurant.name, "restaurant-logo");
										el.setAttribute("style", "width:48px;height:48px");
										return el;
									})()
								: null,
							h("h1", {}, restaurant.name),
							openBadge(restaurant.open),
						),
						h(
							"div",
							{ class: "tags", style: "display:flex;gap:var(--space-2);margin-top:var(--space-2);flex-wrap:wrap" },
							restaurant.restaurantType
								? tag(RESTAURANT_TYPE_LABELS[restaurant.restaurantType])
								: null,
							this.priceCategory
								? h("span", { class: "price-category" }, this.priceCategory)
								: null,
							restaurant.openingHours ? tag(restaurant.openingHours) : null,
							restaurant.fullAddress ? tag(restaurant.fullAddress) : null,
						),
					),
					h("a", { class: "btn btn-secondary", href: "#/" }, "All restaurants"),
				),
				restaurant.fullAddress ? h("div", { style: "margin-bottom:var(--space-6)" }, mapSlot) : null,
				!restaurant.open
					? h(
							"div",
							{ class: "pending-banner", role: "status" },
							h(
								"span",
								{},
								"This restaurant is closed right now. You can browse the menu but not order.",
							),
						)
					: null,
				h("div", { id: "menu-filter-bar" }, this.filterBar()),
				h(
					"div",
					{ class: "menu-layout" },
					h(
						"div",
						{ class: "stack", id: "menu-dish-stack" },
						filtered.length === 0
							? emptyState("No dishes match", "Try clearing the filters.")
							: h(
									"div",
									{ class: "stack" },
									...filtered.map((d) =>
										renderDishCard(restaurant, d, (dish) => void addToCart(restaurant, dish)),
									),
								),
					),
					h("div", { id: "menu-cart-panel" }, cartPanel(restaurant.id)),
				),
			),
		);
	}

	private filterBar(): HTMLElement {
		return menuFilterBar(
			this.filterCategory,
			this.sortOrder,
			(category) => {
				this.filterCategory = category;
				this.repaint();
			},
			(sort) => {
				this.sortOrder = sort;
				this.repaint();
			},
		);
	}

	private repaint(): void {
		if (this.root) this.paint(this.root);
	}

	destroy(): void {
		this.destroyed = true;
		this.unsubscribe?.();
	}
}
