import { getOrder, listOrdersByCustomer, type OrderSummary } from "../../api/orderApi.ts";
import { getRestaurantDetail } from "../../api/restaurantApi.ts";
import { breadcrumb, emptyState, orderBadge, skeletonLines } from "../../presenter/components.ts";
import { h, mount } from "../../presenter/dom.ts";
import { money } from "../../presenter/format.ts";
import { onRecentOrdersChange, recentOrderIds } from "../../infrastructure/recentOrders.ts";
import { getSession } from "../../state/session.ts";
import type { View } from "../View.ts";

const REFRESH_MS = 5000;
const TERMINAL = new Set(["DELIVERED", "REJECTED", "CANCELLED"]);

type Row = { order: OrderSummary; restaurant: string };

/** US21 — all orders for this account, ongoing first. Backed by order-service when logged in. */
export class OrdersListView implements View {
	private destroyed = false;
	private timer: number | null = null;
	private unsubscribe: (() => void) | null = null;

	async render(root: HTMLElement): Promise<void> {
		this.unsubscribe = onRecentOrdersChange(() => void this.load(root, true));
		await this.load(root);
		this.timer = window.setInterval(() => void this.load(root, true), REFRESH_MS);
	}

	private async load(root: HTMLElement, silent = false): Promise<void> {
		const session = getSession();
		let rows: Row[] | null = null;

		if (session) {
			const source = await listOrdersByCustomer(session.sub).catch(() => null);
			if (source !== null) {
				rows = await Promise.all(
					source.map(async (o) => ({
						order: o,
						restaurant: await getRestaurantDetail(o.restaurantId)
							.then((r) => r.name)
							.catch(() => "Restaurant"),
					})),
				);
			}
		} else {
			const ids = recentOrderIds();
			const orders = (await Promise.all(ids.map((id) => getOrder(id).catch(() => null)))).filter(
				(o): o is NonNullable<typeof o> => o !== null,
			);
			rows = await Promise.all(
				orders.map(async (o) => ({
					order: {
						orderId: o.orderId,
						restaurantId: o.restaurantId,
						customerName: o.customerName,
						status: o.status,
						totalAmount: o.totalAmount,
						currency: o.currency,
						placedAt: null,
						itemCount: o.items.length,
						deliveryAddress: o.deliveryAddress,
						items: o.items,
					} as OrderSummary,
					restaurant: await getRestaurantDetail(o.restaurantId)
						.then((r) => r.name)
						.catch(() => "Restaurant"),
				})),
			);
		}

		if (this.destroyed || rows === null) return;
		if (rows.length === 0) {
			if (this.destroyed) return;
			mount(
				root,
				h(
					"div",
					{ class: "view" },
					h(
						"div",
						{ class: "page-header" },
						h("h1", {}, "Track orders"),
						h("p", { class: "subtitle" }, "Your active and recent orders."),
					),
					emptyState(
						"No orders to track",
						"Place an order to start tracking it here.",
						h("a", { class: "btn btn-primary", href: "#/" }, "Browse restaurants"),
					),
				),
			);
			return;
		}

		if (!silent) mount(root, h("div", { class: "view" }, skeletonLines(3)));

		const ongoing = rows.filter((o) => !TERMINAL.has(o.order.status));
		const past = rows.filter((o) => TERMINAL.has(o.order.status));

		mount(
			root,
			h(
				"div",
				{ class: "view" },
				breadcrumb([{ label: "Restaurants", href: "#/" }, { label: "Track orders" }]),
				h(
					"div",
					{ class: "page-header" },
					h(
						"div",
						{},
						h("h1", {}, "Track orders"),
						h("p", { class: "subtitle" }, `${ongoing.length} ongoing · ${past.length} completed`),
					),
				),
				ongoing.length > 0
					? h(
							"div",
							{ class: "section" },
							h("h2", {}, "Ongoing"),
							h("div", { class: "stack" }, ...ongoing.map((o) => this.card(o))),
						)
					: emptyState(
							"No ongoing orders",
							"Your active orders will appear here.",
							h("a", { class: "btn btn-primary", href: "#/" }, "Browse restaurants"),
						),
				past.length > 0
					? h(
							"div",
							{ class: "section" },
							h("h2", {}, "Completed"),
							h("div", { class: "stack" }, ...past.map((o) => this.card(o))),
						)
					: null,
			),
		);
	}

	private card(o: Row): HTMLElement {
		const { order, restaurant } = o;
		return h(
			"a",
			{
				class: "card card-interactive",
				href: `#/orders/${order.orderId}/track`,
			},
			h(
				"div",
				{ class: "split" },
				h(
					"div",
					{},
					h("strong", {}, restaurant),
					h(
						"div",
						{ class: "cluster", style: "margin-top:var(--space-1)" },
						h("span", { class: "mono muted", style: "font-size:var(--text-xs)" }, order.orderId.slice(0, 8)),
						orderBadge(order.status as Parameters<typeof orderBadge>[0]),
					),
					h(
						"p",
						{ class: "muted", style: "margin-top:var(--space-2)" },
						`${order.items.length} item(s) · ${order.items
							.slice(0, 3)
							.map((i) => i.itemName)
							.join(", ")}${order.items.length > 3 ? "…" : ""}`,
					),
				),
				h("span", { class: "price" }, money(order.totalAmount)),
			),
		);
	}

	destroy(): void {
		this.destroyed = true;
		if (this.timer !== null) window.clearInterval(this.timer);
		this.unsubscribe?.();
	}
}
