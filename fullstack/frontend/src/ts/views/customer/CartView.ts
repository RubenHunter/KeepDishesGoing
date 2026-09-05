import { getRestaurantDetail } from "../../api/restaurantApi.ts";
import {
	empty,
	ensureCart,
	currentCart,
	onCartChange,
	removeLine,
	setQuantity,
} from "../../state/cart.ts";
import { breadcrumb, emptyState, stepper, toast } from "../../presenter/components.ts";
import { h, mount } from "../../presenter/dom.ts";
import { money } from "../../presenter/format.ts";
import type { View } from "../View.ts";

/** US15 - review cart before ordering. Server-backed; stepper PATCHes quantity. */
export class CartView implements View {
	private destroyed = false;
	private unsubscribe: (() => void) | null = null;
	private restaurantName: string | null = null;

	async render(root: HTMLElement): Promise<void> {
		this.unsubscribe = onCartChange(() => {
			if (!this.destroyed) this.paint(root);
		});
		try {
			const cart = await ensureCart();
			if (cart.restaurantId) {
				this.restaurantName = await getRestaurantDetail(cart.restaurantId)
					.then((r) => r.name)
					.catch(() => null);
			}
		} catch (error) {
			toast(error instanceof Error ? error.message : "Could not load cart", "error");
		}
		if (!this.destroyed) this.paint(root);
	}

	private paint(root: HTMLElement): void {
		const cart = currentCart();
		const items = cart?.items ?? [];

		if (items.length === 0) {
			mount(
				root,
				h(
					"div",
					{ class: "view" },
					header(),
					emptyState(
						"Your cart is empty",
						"Browse a restaurant and add dishes to your cart.",
						h("a", { class: "btn btn-primary", href: "#/" }, "Browse restaurants"),
					),
				),
			);
			return;
		}

		mount(
			root,
			h(
				"div",
				{ class: "view" },
				header(),
				h(
					"div",
					{ class: "card" },
					h(
						"div",
						{ class: "split" },
						h(
							"div",
							{},
							h("h2", {}, this.restaurantName ?? "Your order"),
							h(
								"p",
								{ class: "muted" },
								"One restaurant per order - your cart holds dishes from this restaurant only.",
							),
						),
						h(
							"button",
							{
								class: "btn btn-ghost btn-sm",
								onclick: () =>
									void empty()
										.then(() => toast("Cart cleared"))
										.catch((e: Error) => toast(e.message, "error")),
							},
							"Clear cart",
						),
					),
					h(
						"div",
						{ class: "cart-lines" },
						...items.map((line) =>
							h(
								"div",
								{ class: "cart-line" },
								h(
									"div",
									{ class: "line-info" },
									h("div", { class: "line-name" }, line.itemName),
									h("div", { class: "line-price" }, `${money(line.unitPrice)} each`),
								),
								stepper(line.quantity, (next) =>
									setQuantity(line.menuItemId, next).catch((e: Error) =>
										toast(e.message, "error"),
									),
								),
								h(
									"span",
									{ class: "price", style: "min-width:80px;text-align:right" },
									money(line.lineTotal),
								),
								h(
									"button",
									{
										class: "btn btn-ghost btn-sm",
										"aria-label": `Remove ${line.itemName}`,
										onclick: () =>
											removeLine(line.menuItemId).catch((e: Error) =>
												toast(e.message, "error"),
											),
									},
									"Remove",
								),
							),
						),
					),
					h(
						"div",
						{
							style:
								"display:flex;justify-content:space-between;padding-top:var(--space-4);border-top:1px solid var(--border);margin-top:var(--space-2)",
						},
						h("strong", {}, "Total"),
						h("span", { class: "price" }, money(cart?.total ?? 0)),
					),
					h(
						"div",
						{ class: "cluster", style: "margin-top:var(--space-4);justify-content:flex-end" },
						cart?.restaurantId
							? h(
									"a",
									{ class: "btn btn-secondary", href: `#/restaurants/${cart.restaurantId}` },
									"Add more dishes",
								)
							: null,
						h("a", { class: "btn btn-primary", href: "#/checkout" }, "Checkout"),
					),
				),
			),
		);
	}

	destroy(): void {
		this.destroyed = true;
		this.unsubscribe?.();
	}
}

function header(): HTMLElement {
	return h(
		"div",
		{},
		breadcrumb([
			{ label: "Restaurants", href: "#/" },
			{ label: "Cart" },
		]),
		h("div", { class: "page-header" }, h("h1", {}, "Your cart")),
	);
}
