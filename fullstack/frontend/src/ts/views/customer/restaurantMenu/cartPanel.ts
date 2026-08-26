import type { Dish } from "../../../domain/Dish.ts";
import type { RestaurantDetail } from "../../../domain/Restaurant.ts";
import { badge, modal, toast } from "../../../presenter/components.ts";
import { h } from "../../../presenter/dom.ts";
import { money } from "../../../presenter/format.ts";
import { add, cartRestaurantId, currentCart, resetCart } from "../../../state/cart.ts";

export function cartPanel(restaurantId: string): HTMLElement {
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

/** US16 - adding from another restaurant replaces the cart (with confirmation). */
export async function addToCart(restaurant: RestaurantDetail, dish: Dish): Promise<void> {
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
