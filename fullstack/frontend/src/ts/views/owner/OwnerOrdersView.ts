import { getOrder } from "../../api/orderApi.ts";
import {
	acceptOrder,
	markOrderReady,
	rejectOrder,
} from "../../api/restaurantApi.ts";
import type { OrderDetail } from "../../domain/Order.ts";
import { recentOrderIds, rememberOrder } from "../../infrastructure/recentOrders.ts";
import { resolveOwnerRestaurantId } from "../../state/ownerRestaurant.ts";
import {
	breadcrumb,
	busyButton,
	emptyState,
	field,
	modal,
	orderBadge,
	toast,
} from "../../presenter/components.ts";
import { h, mount } from "../../presenter/dom.ts";
import { money } from "../../presenter/format.ts";
import type { View } from "../View.ts";

const REFRESH_MS = 5000;

/**
 * US22 (fast decision), US25 (rejection reason), US26 (mark ready).
 * No list endpoint exists backend-side, so the console watches orders placed
 * from this browser plus any order id entered manually (demo flow).
 */
export class OwnerOrdersView implements View {
	private destroyed = false;
	private timer: number | null = null;
	private restaurantId: string | null = null;
	private lastSnapshot = "";

	async render(root: HTMLElement): Promise<void> {
		this.restaurantId = await resolveOwnerRestaurantId();
		if (!this.restaurantId) {
			mount(
				root,
				h(
					"div",
					{ class: "view" },
					emptyState(
						"Create your restaurant first",
						"You need a restaurant before you can handle orders.",
						h("a", { class: "btn btn-primary", href: "#/owner" }, "Go to dashboard"),
					),
				),
			);
			return;
		}
		await this.reload(root);
		this.timer = window.setInterval(() => void this.reload(root, true), REFRESH_MS);
	}

	private async reload(root: HTMLElement, silent = false): Promise<void> {
		const ids = recentOrderIds();
		const orders = (
			await Promise.all(ids.map((id) => getOrder(id).catch(() => null)))
		).filter((o): o is OrderDetail => o !== null);
		if (this.destroyed) return;
		const snapshot = JSON.stringify(orders);
		if (snapshot !== this.lastSnapshot) {
			this.lastSnapshot = snapshot;
			this.paint(root, orders, silent);
		}
	}

	private paint(root: HTMLElement, orders: OrderDetail[], silent: boolean): void {
		const mine = orders.filter(
			(o) => !this.restaurantId || o.restaurantId === this.restaurantId,
		);
		const awaiting = mine.filter((o) => o.status === "PLACED" || o.status === "PENDING");
		const inPrep = mine.filter((o) => o.status === "ACCEPTED");
		const later = mine.filter((o) =>
			["READY_FOR_PICKUP", "PICKED_UP", "DELIVERED", "REJECTED", "CANCELLED"].includes(o.status),
		);

		mount(
			root,
			h(
				"div",
				{ class: "view" },
				breadcrumb([
					{ label: "Dashboard", href: "#/owner" },
					{ label: "Incoming orders" },
				]),
				h(
					"div",
					{ class: "page-header" },
					h(
						"div",
						{},
						h("h1", {}, "Incoming orders"),
						h(
							"p",
							{ class: "subtitle" },
							"Auto-refreshes. Decide within 5 minutes of placement.",
						),
					),
					this.lookupForm(root),
				),
				orders.length === 0 && !silent
					? emptyState(
							"No orders yet",
							"Orders placed from this browser appear here. You can also look up an order by id.",
						)
					: null,
				awaiting.length > 0
					? h(
							"div",
							{ class: "section" },
							h("h2", {}, `Awaiting decision (${awaiting.length})`),
							h("div", { class: "stack" }, ...awaiting.map((o) => this.decisionCard(root, o))),
						)
					: null,
				inPrep.length > 0
					? h(
							"div",
							{ class: "section" },
							h("h2", {}, `In preparation (${inPrep.length})`),
							h("div", { class: "stack" }, ...inPrep.map((o) => this.prepCard(root, o))),
						)
					: null,
				later.length > 0
					? h(
							"div",
							{ class: "section" },
							h("h2", {}, "Earlier"),
							h(
								"div",
								{ class: "table-wrap" },
								h(
									"table",
									{ class: "table" },
									h(
										"thead",
										{},
										h(
											"tr",
											{},
											h("th", {}, "Customer"),
											h("th", { class: "num" }, "Items"),
											h("th", { class: "num" }, "Total"),
											h("th", {}, "Status"),
										),
									),
									h(
										"tbody",
										{},
										...later.map((o) =>
											h(
												"tr",
												{},
												h("td", {}, o.customerName),
												h("td", { class: "num" }, String(o.items.length)),
												h("td", { class: "num" }, money(o.totalAmount)),
												h("td", {}, orderBadge(o.status)),
											),
										),
									),
								),
							),
						)
					: null,
			),
		);
	}

	private lookupForm(root: HTMLElement): HTMLElement {
		const input = h("input", {
			class: "input",
			placeholder: "Order id…",
			style: "width:280px",
		});
		return h(
			"form",
			{
				class: "cluster",
				onsubmit: (e: Event) => {
					e.preventDefault();
					const id = input.value.trim();
					if (!id) return;
					rememberOrder(id);
					input.value = "";
					void this.reload(root, true);
				},
			},
			input,
			h("button", { class: "btn btn-secondary", type: "submit" }, "Watch order"),
		);
	}

	private decisionCard(root: HTMLElement, order: OrderDetail): HTMLElement {
		return h(
			"div",
			{ class: "card" },
			h(
				"div",
				{ class: "split" },
				h(
					"div",
					{},
					h(
						"div",
						{ class: "cluster" },
						h("strong", {}, order.customerName),
						orderBadge(order.status),
					),
					h(
						"ul",
						{ style: "margin-top:var(--space-2)" },
						...order.items.map((i) => h("li", {}, `${i.quantity}× ${i.itemName}`)),
					),
					order.deliveryAddress
						? h("p", { class: "muted", style: "margin-top:var(--space-2)" }, order.deliveryAddress)
						: null,
				),
				h("div", { class: "price" }, money(order.totalAmount)),
			),
			h(
				"div",
				{ class: "cluster", style: "margin-top:var(--space-4);justify-content:flex-end" },
				h(
					"button",
					{ class: "btn btn-danger", onclick: () => this.rejectDialog(root, order) },
					"Reject",
				),
				h(
					"button",
					{
						class: "btn btn-primary",
						onclick: (e: Event) =>
							void this.run(
								e.target as HTMLButtonElement,
								() => acceptOrder(this.restaurantId!, order.orderId, undefined, order.deliveryAddress ?? undefined),
								"Order accepted - a courier can now claim it",
								root,
							),
					},
					"Accept",
				),
			),
		);
	}

	private prepCard(root: HTMLElement, order: OrderDetail): HTMLElement {
		return h(
			"div",
			{ class: "card" },
			h(
				"div",
				{ class: "split" },
				h(
					"div",
					{},
					h(
						"div",
						{ class: "cluster" },
						h("strong", {}, order.customerName),
						orderBadge(order.status),
					),
					h(
						"ul",
						{ style: "margin-top:var(--space-2)" },
						...order.items.map((i) => h("li", {}, `${i.quantity}× ${i.itemName}`)),
					),
				),
				h(
					"button",
					{
						class: "btn btn-primary",
						onclick: (e: Event) =>
							void this.run(
								e.target as HTMLButtonElement,
								() => markOrderReady(this.restaurantId!, order.orderId),
								"Marked ready for pickup",
								root,
							),
					},
					"Ready for pickup",
				),
			),
		);
	}

	private rejectDialog(root: HTMLElement, order: OrderDetail): void {
		const reason = h("textarea", {
			class: "textarea",
			required: true,
			placeholder: "e.g. Ingredient sold out",
		});
		const rejectBtn = h("button", { class: "btn btn-danger", type: "submit" }, "Reject order");
		const form = h(
			"form",
			{
				onsubmit: (e: Event) => {
					e.preventDefault();
					dialog.close();
					void this.run(
						rejectBtn,
						() => rejectOrder(this.restaurantId!, order.orderId, reason.value),
						"Order rejected",
						root,
					);
				},
			},
			field("Reason (shown to the customer)", reason),
			rejectBtn,
		);
		const dialog = modal("Reject order", form, []);
	}

	private async run(
		btn: HTMLButtonElement,
		action: () => Promise<unknown>,
		successMessage: string,
		root: HTMLElement,
	): Promise<void> {
		busyButton(btn, true);
		try {
			await action();
			toast(successMessage, "success");
			window.setTimeout(() => void this.reload(root, true), 1500); // let AMQP settle
		} catch (error) {
			busyButton(btn, false);
			toast(error instanceof Error ? error.message : "Action failed", "error");
		}
	}

	destroy(): void {
		this.destroyed = true;
		if (this.timer !== null) window.clearInterval(this.timer);
	}
}
