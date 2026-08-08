import { getOrder } from "../../api/orderApi.ts";
import { getRestaurantDetail } from "../../api/restaurantApi.ts";
import { ORDER_DECISION_WINDOW_MIN } from "../../config.ts";
import { emptyState, orderBadge, spinner } from "../../presenter/components.ts";
import { h, mount } from "../../presenter/dom.ts";
import { money } from "../../presenter/format.ts";
import { trackOrder } from "../../state/trackedOrder.ts";
import type { View } from "../View.ts";

/** Post-payment confirmation (US18 locked, link to tracking US21). */
export class OrderConfirmationView implements View {
	private destroyed = false;

	async render(root: HTMLElement, params: Record<string, string>): Promise<void> {
		const orderId = params.id;
		mount(
			root,
			h(
				"div",
				{ class: "view empty", style: "min-height:40vh" },
				spinner(),
				h("p", { class: "muted" }, "Confirming your order…"),
			),
		);

		try {
			const order = await getOrder(orderId);
			trackOrder(order.orderId);
			const restaurantName = await getRestaurantDetail(order.restaurantId)
				.then((r) => r.name)
				.catch(() => "the restaurant");
			if (this.destroyed) return;
			mount(
				root,
				h(
					"div",
					{ class: "view" },
					h(
						"div",
						{ class: "card", style: "max-width:640px;margin-inline:auto" },
						h(
							"div",
							{ class: "cluster" },
							h("h1", {}, "Order placed"),
							orderBadge(order.status),
						),
						h(
							"p",
							{ class: "muted", style: "margin-block:var(--space-3)" },
							`Thanks ${order.customerName} - ${restaurantName} has ${ORDER_DECISION_WINDOW_MIN} minutes to accept your order. Content and price are now locked.`,
						),
						h(
							"dl",
							{ class: "order-meta" },
							h("dt", {}, "Order"),
							h("dd", { class: "mono" }, order.orderId),
							h("dt", {}, "Total"),
							h("dd", { class: "price" }, money(order.totalAmount)),
							order.deliveryAddress
								? h("dt", {}, "Delivery address")
								: null,
							order.deliveryAddress ? h("dd", {}, order.deliveryAddress) : null,
						),
						h(
							"div",
							{ class: "cluster", style: "margin-top:var(--space-4)" },
							h(
								"a",
								{ class: "btn btn-primary", href: `#/orders/${order.orderId}/track` },
								"Track order",
							),
							h("a", { class: "btn btn-secondary", href: "#/" }, "Back to restaurants"),
						),
					),
				),
			);
		} catch (error) {
			if (this.destroyed) return;
			mount(
				root,
				h(
					"div",
					{ class: "view" },
					emptyState(
						"Could not load your order",
						error instanceof Error ? error.message : "Unknown error",
						h("a", { class: "btn btn-primary", href: "#/" }, "Back to restaurants"),
					),
				),
			);
		}
	}

	destroy(): void {
		this.destroyed = true;
	}
}
