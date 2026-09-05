import { cancelOrder, getOrder, getTracking } from "../../api/orderApi.ts";
import { getRestaurantDetail } from "../../api/restaurantApi.ts";
import { TRACKING_POLL_MS } from "../../config.ts";
import type { OrderStatus, Tracking } from "../../domain/Order.ts";
import {
	breadcrumb,
	busyButton,
	emptyState,
	modal,
	orderBadge,
	spinner,
	toast,
} from "../../presenter/components.ts";
import { h, mount } from "../../presenter/dom.ts";
import { money, timeOnly } from "../../presenter/format.ts";
import type { View } from "../View.ts";

type StepDef = { status: OrderStatus; label: string; at: (t: Tracking) => string | null };

const LIFECYCLE: StepDef[] = [
	{ status: "PLACED", label: "Order placed", at: (t) => t.placedAt },
	{ status: "ACCEPTED", label: "Accepted by restaurant", at: (t) => t.acceptedAt },
	{ status: "READY_FOR_PICKUP", label: "Ready for pickup", at: (t) => t.readyAt },
	{ status: "PICKED_UP", label: "Courier on its way", at: (t) => t.pickedUpAt },
	{ status: "DELIVERED", label: "Delivered", at: (t) => t.deliveredAt },
];

/** US21 (tracking), US25 (rejection reason), US33 (delivery progress). Polls every 5s. */
export class OrderTrackingView implements View {
	private destroyed = false;
	private timer: number | null = null;
	private lastStatus: OrderStatus | null = null;
	private lastSnapshot = "";
	private restaurantName: string | null = null;

	async render(root: HTMLElement, params: Record<string, string>): Promise<void> {
		const orderId = params.id;
		await this.poll(root, orderId);
		this.timer = window.setInterval(() => void this.poll(root, orderId), TRACKING_POLL_MS);
	}

	private async poll(root: HTMLElement, orderId: string): Promise<void> {
		try {
			const tracking = await getTracking(orderId);
			if (this.destroyed) return;

			// Fetch restaurant name once
			if (!this.restaurantName) {
				try {
					const order = await getOrder(orderId);
					const detail = await getRestaurantDetail(order.restaurantId);
					this.restaurantName = detail.name;
				} catch {
					this.restaurantName = "Restaurant";
				}
			}
			const snapshot = JSON.stringify(tracking);
			if (snapshot !== this.lastSnapshot) {
				this.lastSnapshot = snapshot;
				this.paint(root, orderId, tracking);
			}
			if (isTerminal(tracking.status) && this.timer !== null) {
				window.clearInterval(this.timer);
				this.timer = null;
			}
		} catch (error) {
			if (this.destroyed || this.lastStatus !== null) return; // keep last good render
			mount(
				root,
				h(
					"div",
					{ class: "view" },
					emptyState(
						"Could not load this order",
						error instanceof Error ? error.message : "Unknown error",
						h("a", { class: "btn btn-primary", href: "#/" }, "Back to restaurants"),
					),
				),
			);
		}
	}

	private paint(root: HTMLElement, orderId: string, t: Tracking): void {
		const statusChanged = this.lastStatus !== null && this.lastStatus !== t.status;
		this.lastStatus = t.status;

		const badgeEl = orderBadge(t.status);
		if (statusChanged) badgeEl.classList.add("flash");

		const rejected = t.status === "REJECTED";
		const cancelled = t.status === "CANCELLED";
		const currentIndex = LIFECYCLE.findIndex((s) => s.status === t.status);

		mount(
			root,
			h(
				"div",
				{ class: "view" },
				breadcrumb([
					{ label: "Restaurants", href: "#/" },
					{ label: "Track orders", href: "#/orders" },
					{ label: this.restaurantName ?? "Restaurant" },
					{ label: "Tracking" },
				]),
				h(
					"div",
					{ class: "page-header" },
					h(
						"div",
						{},
						h("h1", {}, this.restaurantName ?? "Order tracking"),
						h("p", { class: "subtitle mono" }, orderId),
					),
					badgeEl,
				),
				h(
					"div",
					{ class: "tracking-layout" },
					h(
						"div",
						{ class: "card" },
						rejected || cancelled
							? this.terminalBlock(t, rejected)
							: h(
									"div",
									{ class: "timeline" },
									...LIFECYCLE.map((step, index) => {
										const state =
											index < currentIndex || t.status === "DELIVERED"
												? "done"
												: index === currentIndex
													? "active"
													: "pending";
										const at = step.at(t);
										return this.step(step.label, at ? timeOnly(at) : null, state);
									}),
								),
					),
					this.summaryCard(orderId),
					!isTerminal(t.status)
						? h(
								"p",
								{ class: "muted", style: "display:flex;align-items:center;gap:var(--space-2)" },
								spinner(),
								"This page refreshes automatically.",
							)
						: null,
				),
			),
		);
	}

	private terminalBlock(t: Tracking, rejected: boolean): HTMLElement {
		return h(
			"div",
			{ class: "stack" },
			h(
				"div",
				{ class: "timeline" },
				this.step("Order placed", t.placedAt ? timeOnly(t.placedAt) : null, "done"),
				this.step(
					rejected ? "Rejected by restaurant" : "Order cancelled",
					null,
					"rejected",
				),
			),
			h(
				"div",
				{
					class: "card",
					style: "background:var(--danger-soft);border-color:var(--danger)",
				},
				h("strong", {}, "Reason: "),
				t.rejectReason ?? "No reason given.",
				rejected
					? h(
							"p",
							{ class: "muted", style: "margin-top:var(--space-2)" },
							"You can adjust your cart or try again later.",
						)
					: null,
			),
			h(
				"a",
				{ class: "btn btn-primary", href: "#/", style: "align-self:flex-start" },
				"Back to restaurants",
			),
		);
	}

	private summaryCard(orderId: string): HTMLElement {
		const card = h("div", { class: "card" }, h("h2", {}, "Your order"), spinner());
		void getOrder(orderId)
			.then((order) => {
				const rows = order.items.map((item) =>
					h(
						"div",
						{
							class: "row",
							style: "display:flex;justify-content:space-between;padding-block:var(--space-1)",
						},
						h("span", {}, `${item.quantity}× ${item.itemName}`),
						h("span", { class: "mono" }, money(item.unitPrice * item.quantity)),
					),
				);
				const canCancel =
					order.status === "PENDING" ||
					order.status === "PLACED" ||
					order.status === "ACCEPTED";
				mount(
					card,
					h("h2", { style: "margin-bottom:var(--space-3)" }, "Your order"),
					...rows,
					h(
						"div",
						{
							style:
								"display:flex;justify-content:space-between;border-top:1px solid var(--border);margin-top:var(--space-2);padding-top:var(--space-2)",
						},
						h("strong", {}, "Total"),
						h("span", { class: "price" }, money(order.totalAmount)),
					),
					canCancel
						? h(
								"div",
								{ style: "margin-top:var(--space-4);text-align:right" },
								h(
									"button",
									{
										class: "btn btn-ghost btn-sm",
										onclick: () => this.cancelDialog(orderId),
									},
									"Cancel order",
								),
							)
						: null,
				);
			})
			.catch(() => mount(card, h("p", { class: "muted" }, "Order details unavailable.")));
		return card;
	}

	private cancelDialog(orderId: string): void {
		const reason = h("input", { class: "input", required: true, placeholder: "Changed my mind" });
		const confirmBtn = h("button", { class: "btn btn-danger", type: "submit" }, "Cancel order");
		const form = h(
			"form",
			{
				onsubmit: (e: Event) => {
					e.preventDefault();
					dialog.close();
					void (async () => {
						busyButton(confirmBtn, true);
						try {
							await cancelOrder(orderId, reason.value);
							toast("Order cancelled", "success");
						} catch (error) {
							toast(error instanceof Error ? error.message : "Cancel failed", "error");
						}
					})();
				},
			},
			h("div", { class: "field" }, h("label", {}, "Reason"), reason),
			confirmBtn,
		);
		const dialog = modal("Cancel this order?", form, []);
	}

	private step(
		label: string,
		time: string | null,
		state: "done" | "active" | "pending" | "rejected",
	): HTMLElement {
		return h(
			"div",
			{ class: `timeline-step ${state}` },
			h("span", { class: "timeline-dot", "aria-hidden": "true" }),
			h(
				"div",
				{},
				h("div", { class: "step-label" }, label),
				time ? h("div", { class: "step-time" }, time) : null,
			),
		);
	}

	destroy(): void {
		this.destroyed = true;
		if (this.timer !== null) window.clearInterval(this.timer);
	}
}

function isTerminal(status: OrderStatus): boolean {
	return status === "DELIVERED" || status === "REJECTED" || status === "CANCELLED";
}
