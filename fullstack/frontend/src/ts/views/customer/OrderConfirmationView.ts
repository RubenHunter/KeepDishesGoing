import { getOrder, placeOrder } from "../../api/orderApi.ts";
import { getRestaurantDetail } from "../../api/restaurantApi.ts";
import { ORDER_DECISION_WINDOW_MIN, PAYMENT_POLL_MAX_ATTEMPTS, PAYMENT_POLL_MS } from "../../config.ts";
import type { OrderDetail } from "../../domain/Order.ts";
import { emptyState, orderBadge, spinner } from "../../presenter/components.ts";
import { h, mount } from "../../presenter/dom.ts";
import { money } from "../../presenter/format.ts";
import type { View } from "../View.ts";

/**
 * Post-payment confirmation. Stripe redirects here on success; the webhook may not have marked the
 * order PAID yet, so we poll the order until PAID, place it (US18 lock + US23 window starts), then
 * show the "Order placed" card.
 */
export class OrderConfirmationView implements View {
	private destroyed = false;

	async render(root: HTMLElement, params: Record<string, string>): Promise<void> {
		const orderId = params.id;
		this.mountSpinner(root);

		try {
			let order = await getOrder(orderId);

			// Stripe redirect races the webhook: wait for payment to settle before placing.
			if (order.status === "PENDING" && order.paymentStatus !== "PAID") {
				const paid = await this.pollUntilPaid(orderId);
				if (paid === null) {
					this.mountPaymentPending(root, params);
					return;
				}
				order = paid;
			}

			if (order.status === "PENDING" && order.paymentStatus === "PAID") {
				await placeOrder(orderId);
				order = await getOrder(orderId);
			}

			const restaurantName = await getRestaurantDetail(order.restaurantId)
				.then((r) => r.name)
				.catch(() => "the restaurant");
			if (this.destroyed) return;
			this.mountPlaced(root, order, restaurantName);
		} catch (error) {
			if (this.destroyed) return;
			this.mountError(root, error);
		}
	}

	private async pollUntilPaid(orderId: string): Promise<OrderDetail | null> {
		for (let attempt = 0; attempt < PAYMENT_POLL_MAX_ATTEMPTS; attempt++) {
			await new Promise((resolve) => setTimeout(resolve, PAYMENT_POLL_MS));
			if (this.destroyed) return null;
			const order = await getOrder(orderId);
			if (order.paymentStatus === "PAID") return order;
		}
		return null;
	}

	private mountSpinner(root: HTMLElement): void {
		mount(
			root,
			h(
				"div",
				{ class: "view empty", style: "min-height:40vh" },
				spinner(),
				h("p", { class: "muted" }, "Confirming your order…"),
			),
		);
	}

	private mountPaymentPending(root: HTMLElement, params: Record<string, string>): void {
		mount(
			root,
			h(
				"div",
				{ class: "view" },
				h(
					"div",
					{ class: "card", style: "max-width:640px;margin-inline:auto" },
					h("h1", {}, "Payment not completed"),
					h(
						"p",
						{ class: "muted", style: "margin-block:var(--space-3)" },
						"Your payment has not been confirmed yet. You can try again or return to your cart.",
					),
					h(
						"div",
						{ class: "cluster", style: "margin-top:var(--space-4)" },
						h(
							"button",
							{
								class: "btn btn-primary",
								onclick: () => void this.render(root, params),
							},
							"Try again",
						),
						h("a", { class: "btn btn-secondary", href: "#/cart" }, "Back to cart"),
					),
				),
			),
		);
	}

	private mountPlaced(root: HTMLElement, order: OrderDetail, restaurantName: string): void {
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
	}

	private mountError(root: HTMLElement, error: unknown): void {
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

	destroy(): void {
		this.destroyed = true;
	}
}
