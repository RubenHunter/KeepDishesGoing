import {
	cancelClaim,
	getDelivery,
	markDelivered,
	markInTransit,
	markPickedUp,
} from "../../api/deliveryApi.ts";
import type { Delivery } from "../../domain/Delivery.ts";
import {
	breadcrumb,
	busyButton,
	deliveryBadge,
	emptyState,
	field,
	modal,
	spinner,
	toast,
} from "../../presenter/components.ts";
import { h, mount } from "../../presenter/dom.ts";
import { timeOnly } from "../../presenter/format.ts";
import { restaurantNameForOrder } from "../../state/restaurantNames.ts";
import type { View } from "../View.ts";

const REFRESH_MS = 5000;

/**
 * US29 (cancel claim while not ready), US30 (pickup → transit → deliver),
 * US34 (payout credited on delivery).
 */
export class DeliveryDetailView implements View {
	private destroyed = false;
	private timer: number | null = null;
	private deliveryId = "";
	private lastSnapshot = "";

	async render(root: HTMLElement, params: Record<string, string>): Promise<void> {
		this.deliveryId = params.id;
		mount(root, h("div", { class: "view empty", style: "min-height:30vh" }, spinner()));
		await this.reload(root, this.deliveryId);
		this.timer = window.setInterval(
			() => void this.reload(root, this.deliveryId, true),
			REFRESH_MS,
		);
	}

	private async reload(root: HTMLElement, deliveryId: string, silent = false): Promise<void> {
		try {
			const delivery = await getDelivery(deliveryId);
			if (this.destroyed) return;
			const snapshot = JSON.stringify(delivery);
			if (snapshot !== this.lastSnapshot) {
				this.lastSnapshot = snapshot;
				this.paint(root, delivery);
			}
		} catch (error) {
			if (this.destroyed || silent) return;
			mount(
				root,
				h(
					"div",
					{ class: "view" },
					emptyState(
						"Could not load this delivery",
						error instanceof Error ? error.message : "Unknown error",
						h("a", { class: "btn btn-primary", href: "#/driver" }, "Back to deliveries"),
					),
				),
			);
		}
	}

	private paint(root: HTMLElement, d: Delivery): void {
		const terminal = d.status === "DELIVERED" || d.status === "CANCELLED";
		if (terminal && this.timer !== null) {
			window.clearInterval(this.timer);
			this.timer = null;
		}

		mount(
			root,
			h(
				"div",
				{ class: "view" },
				breadcrumb([
					{ label: "Deliveries", href: "#/driver" },
					{ label: d.orderId.slice(0, 8) },
				]),
				h(
					"div",
					{ class: "page-header" },
					h(
						"div",
						{},
						h("h1", {}, `Delivery`),
						h("p", { class: "subtitle mono" }, `Order ${d.orderId}`),
						h("p", { class: "subtitle addr-restaurant", id: "resname-detail" }),
					),
					deliveryBadge(d.status),
				),
				h(
					"div",
					{ class: "tracking-layout" },
					h(
						"div",
						{ class: "card delivery-card" },
						h(
							"div",
							{ class: "addresses" },
							h(
								"div",
								{},
								h("div", { class: "addr-label" }, "Pickup from"),
								d.pickupAddress,
							),
							h(
								"div",
								{},
								h("div", { class: "addr-label" }, "Deliver to"),
								d.deliveryAddress,
							),
						),
						h(
							"dl",
							{ class: "order-meta", style: "margin-top:var(--space-4)" },
							...this.timelineFields(d),
						),
					),
					h("div", { class: "cluster" }, ...this.actions(root, d)),
				),
			),
		);

		void restaurantNameForOrder(d.orderId).then((name) => {
			if (this.destroyed || !name) return;
			const el = document.getElementById("resname-detail");
			if (el) el.textContent = `Pickup at ${name}`;
		});
	}

	private timelineFields(d: Delivery): HTMLElement[] {
		const fields: HTMLElement[] = [];
		const add = (label: string, value: string | null): void => {
			if (value) {
				fields.push(h("dt", {}, label), h("dd", {}, timeOnly(value)));
			}
		};
		add("Claimed at", d.assignedAt);
		add("Ready at", d.readyAt);
		add("Picked up at", d.pickedUpAt);
		add("In transit at", d.inTransitAt);
		add("Delivered at", d.deliveredAt);
		add("Cancelled at", d.cancelledAt);
		if (d.cancellationReason) {
			fields.push(h("dt", {}, "Cancellation reason"), h("dd", {}, d.cancellationReason));
		}
		return fields;
	}

	private actions(root: HTMLElement, d: Delivery): HTMLElement[] {
		switch (d.status) {
			case "ASSIGNED":
				return [
					h(
						"button",
						{
							class: "btn btn-danger",
							onclick: () => this.cancelDialog(d),
						},
						"Cancel claim",
					),
					h(
						"p",
						{ class: "muted" },
						"Waiting for the restaurant to mark the order ready. You can still release your claim.",
					),
				];
			case "READY_FOR_PICKUP":
				return [
					h(
						"button",
						{
							class: "btn btn-primary",
							onclick: (e: Event) =>
								void this.run(root, e.target as HTMLButtonElement, () =>
									markPickedUp(d.deliveryId),
								),
						},
						"Mark picked up",
					),
				];
			case "PICKED_UP":
				return [
					h(
						"button",
						{
							class: "btn btn-primary",
							onclick: (e: Event) =>
								void this.run(root, e.target as HTMLButtonElement, () =>
									markInTransit(d.deliveryId),
								),
						},
						"Start transit",
					),
				];
			case "IN_TRANSIT":
				return [
					h(
						"button",
						{
							class: "btn btn-primary",
							onclick: (e: Event) =>
								void this.run(root, e.target as HTMLButtonElement, () =>
									markDelivered(d.deliveryId),
								),
						},
						"Mark delivered",
					),
				];
			default:
				return [h("a", { class: "btn btn-secondary", href: "#/driver" }, "Back to deliveries")];
		}
	}

	private cancelDialog(d: Delivery): void {
		const reason = h("input", {
			class: "input",
			required: true,
			placeholder: "e.g. Vehicle trouble",
		});
		const confirmBtn = h(
			"button",
			{ class: "btn btn-danger", type: "submit" },
			"Release claim",
		);
		const form = h(
			"form",
			{
				onsubmit: (e: Event) => {
					e.preventDefault();
					dialog.close();
					void (async () => {
						busyButton(confirmBtn, true);
						try {
							await cancelClaim(d.deliveryId, reason.value);
							toast("Claim released", "success");
							location.hash = "#/driver";
						} catch (error) {
							busyButton(confirmBtn, false);
							toast(error instanceof Error ? error.message : "Release failed", "error");
						}
					})();
				},
			},
			field("Reason", reason),
			confirmBtn,
		);
		const dialog = modal("Release this claim?", form, []);
	}

	private async run(
		root: HTMLElement,
		btn: HTMLButtonElement,
		action: () => Promise<unknown>,
	): Promise<void> {
		busyButton(btn, true);
		try {
			await action();
			toast("Status updated", "success");
			await this.reload(root, this.deliveryId, true);
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
