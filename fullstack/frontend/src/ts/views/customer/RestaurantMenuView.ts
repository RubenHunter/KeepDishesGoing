import {
	getMenu,
	getPriceCategory,
	getRestaurantDetail,
	getRestaurantStatus,
} from "../../api/restaurantApi.ts";
import { DISH_CATEGORY_LABELS, type Dish, type DishCategory } from "../../domain/Dish.ts";
import type { RestaurantDetail } from "../../domain/Restaurant.ts";
import {
	add,
	cartRestaurantId,
	currentCart,
	ensureCart,
	onCartChange,
	resetCart,
} from "../../state/cart.ts";
import {
	badge,
	breadcrumb,
	dishBadge,
	dishImg,
	emptyState,
	logoImg,
	modal,
	openBadge,
	skeletonLines,
	tag,
	toast,
} from "../../presenter/components.ts";
import { h, mount } from "../../presenter/dom.ts";
import { money } from "../../presenter/format.ts";
import { restaurantMap } from "../../presenter/map.ts";
import { RESTAURANT_TYPE_LABELS } from "../../domain/Restaurant.ts";
import type { View } from "../View.ts";

/** US15 (one restaurant at a time), US16 (single-restaurant cart), dish states. */
export class RestaurantMenuView implements View {
	private destroyed = false;
	private unsubscribe: (() => void) | null = null;
	private restaurant: RestaurantDetail | null = null;
	private priceCategory: string | null = null;
	private filterCategory: DishCategory | null = null;
	private menuData: Dish[] = [];
	private sortOrder: "none" | "asc" | "desc" = "none";

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
			await ensureCart();
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
		const restaurant = this.restaurant;
		if (!restaurant) return;

		const filtered = this.filteredMenu();

		// If already painted, only update the dish list + filters
		const existingStack = document.getElementById("menu-dish-stack");
		const existingFilter = document.getElementById("menu-filter-bar");
		if (existingStack && existingFilter) {
			mount(existingFilter, this.menuFilterBar(root));
			if (filtered.length === 0) {
				mount(existingStack, emptyState("No dishes match", "Try clearing the filters."));
			} else {
				mount(existingStack, h("div", { class: "stack" }, ...filtered.map((d) => this.dishCard(restaurant, d))));
			}
			const cartSlot = document.getElementById("menu-cart-panel");
			if (cartSlot && this.restaurant) mount(cartSlot, this.cartPanel(this.restaurant.id));
			return;
		}

		// First render - always compute distance

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
			h("div", { id: "menu-filter-bar" }, this.menuFilterBar(root)),
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
								...filtered.map((d) => this.dishCard(restaurant, d)),
							),
				),
				h("div", { id: "menu-cart-panel" }, this.cartPanel(restaurant.id)),
			),
			),
		);
	}

	private dishCard(restaurant: RestaurantDetail, dish: Dish): HTMLElement {
		const orderable = dish.status === "PUBLISHED" && restaurant.open;
		return h(
			"div",
			{ class: `card dish-card ${dish.status === "OUT_OF_STOCK" ? "dish-unavailable" : ""}` },
			dish.imageUrl ? dishImg(dish.imageUrl, dish.name) : null,
			h(
				"div",
				{},
				h(
					"div",
					{ class: "dish-name" },
					dish.name,
					dish.status === "OUT_OF_STOCK" ? dishBadge("OUT_OF_STOCK") : null,
					dish.category
						? h("span", { class: "badge badge-neutral" }, DISH_CATEGORY_LABELS[dish.category])
						: null,
				),
				h("p", { class: "dish-desc" }, dish.description),
			),
			h(
				"div",
				{ class: "dish-side" },
				h("span", { class: "price" }, money(dish.price.amount)),
				h(
					"button",
					{
						class: "btn btn-primary btn-sm",
						disabled: !orderable,
						onclick: () => void this.onAdd(restaurant, dish),
					},
					"Add",
				),
			),
		);
	}

	/** US16 - adding from another restaurant replaces the cart (with confirmation). */
	private async onAdd(restaurant: RestaurantDetail, dish: Dish): Promise<void> {
		const otherRestaurant =
			cartRestaurantId() !== null && cartRestaurantId() !== restaurant.id;

		const doAdd = async (): Promise<void> => {
			try {
				if (otherRestaurant) await resetCart();
				await add(restaurant.id, {
					menuItemId: dish.id,
					itemName: dish.name,
					unitPrice: dish.price.amount,
				});
				toast(`${dish.name} added to cart`, "success");
			} catch (error) {
				toast(error instanceof Error ? error.message : "Could not add dish", "error");
			}
		};

		if (!otherRestaurant) {
			await doAdd();
			return;
		}

		const confirmBtn = h(
			"button",
			{ class: "btn btn-danger", onclick: () => { dialog.close(); void doAdd(); } },
			"Replace cart",
		);
		const dialog = modal(
			"Start a new cart?",
			h(
				"p",
				{},
				"Your cart holds dishes from another restaurant. One restaurant per order - adding this dish replaces your current cart.",
			),
			[confirmBtn],
		);
	}

	private cartPanel(restaurantId: string): HTMLElement {
		const cart = currentCart();
		const items = cart?.items ?? [];
		if (items.length === 0) {
			return h(
				"aside",
				{ class: "card cart-summary" },
				h("h2", {}, "Your cart"),
				h("p", { class: "muted" }, "Add dishes to start an order."),
			);
		}

		// US16 - cart belongs to another restaurant: never show its lines here.
		if (cart?.restaurantId !== restaurantId) {
			return h(
				"aside",
				{ class: "card cart-summary" },
				h("h2", {}, "Your cart"),
				h(
					"p",
					{},
					badge("Other restaurant", "warning"),
					h(
						"span",
						{ class: "help muted" },
						" - you have a cart at another restaurant. Adding a dish here replaces it (one restaurant per order).",
					),
				),
				h(
					"a",
					{ class: "btn btn-secondary", href: "#/cart", style: "width:100%;margin-top:var(--space-3)" },
					"View that cart",
				),
			);
		}

		return h(
			"aside",
			{ class: "card cart-summary" },
			h("h2", {}, "Your cart"),
			...items.map((item) =>
				h(
					"div",
					{ class: "row" },
					h("span", {}, `${item.quantity}× ${item.itemName}`),
					h("span", { class: "mono" }, money(item.lineTotal)),
				),
			),
			h(
				"div",
				{ class: "row total" },
				h("span", {}, "Total"),
				h("span", { class: "price" }, money(cart?.total ?? 0)),
			),
			h("a", { class: "btn btn-primary", href: "#/cart", style: "width:100%" }, "Review cart"),
		);
	}

	private filteredMenu(): Dish[] {
		let dishes = this.menuData;
		if (this.filterCategory) {
			dishes = dishes.filter((d) => d.category === this.filterCategory);
		}
		if (this.sortOrder !== "none") {
			dishes = [...dishes].sort((a, b) =>
				this.sortOrder === "asc"
					? a.price.amount - b.price.amount
					: b.price.amount - a.price.amount,
			);
		}
		return dishes;
	}

	private menuFilterBar(root: HTMLElement): HTMLElement {
		const chips: HTMLElement[] = [
			h(
				"button",
				{
					class: `filter-chip ${this.filterCategory === null ? "active" : ""}`,
					"aria-pressed": String(this.filterCategory === null),
					onclick: () => { this.filterCategory = null; this.paint(root); },
				},
				"All",
			),
		];
		for (const [cat, label] of Object.entries(DISH_CATEGORY_LABELS)) {
			chips.push(
				h(
					"button",
					{
						class: `filter-chip ${this.filterCategory === cat ? "active" : ""}`,
						"aria-pressed": String(this.filterCategory === cat),
						onclick: () => {
							this.filterCategory = this.filterCategory === cat ? null : (cat as DishCategory);
			this.paint(root);
						},
					},
					label,
				),
			);
		}
		const sortSelect = h(
			"select",
			{
				class: "select",
				style: "max-width:160px;height:32px;font-size:var(--text-sm)",
				onchange: (e: Event) => {
					this.sortOrder = (e.target as HTMLSelectElement).value as "none" | "asc" | "desc";
					this.paint(root);
				},
			},
			h("option", { value: "none", selected: this.sortOrder === "none" }, "Sort price"),
			h("option", { value: "asc", selected: this.sortOrder === "asc" }, "Lowest first"),
			h("option", { value: "desc", selected: this.sortOrder === "desc" }, "Highest first"),
		);
		return h(
			"div",
			{ class: "filter-bar", role: "group", "aria-label": "Menu filters" },
			...chips,
			sortSelect,
		);
	}

	destroy(): void {
		this.destroyed = true;
		this.unsubscribe?.();
	}
}
