import {
	claimDelivery,
	listAvailableDeliveries,
	listMyDeliveries,
	registerDriver,
} from "../../api/deliveryApi.ts";
import type { Delivery } from "../../domain/Delivery.ts";
import { getSession } from "../../state/session.ts";
import { restaurantNameForOrder } from "../../state/restaurantNames.ts";
import { geocode, haversineKm } from "../../presenter/geo.ts";
import {
	busyButton,
	deliveryBadge,
	emptyState,
	skeletonLines,
	toast,
} from "../../presenter/components.ts";
import { h, mount } from "../../presenter/dom.ts";
import { money } from "../../presenter/format.ts";
import type { View } from "../View.ts";

const REFRESH_MS = 5000;
const ACTIVE_STATUSES = ["ASSIGNED", "READY_FOR_PICKUP", "PICKED_UP", "IN_TRANSIT"];

/** US27/US28 (claim available), US31 (one active at a time). */
export class DriverDeliveriesView implements View {
	private destroyed = false;
	private timer: number | null = null;
	private lastSnapshot = "";

	async render(root: HTMLElement): Promise<void> {
		mount(root, h("div", { class: "view" }, skeletonLines(4)));
		await this.reload(root);
		this.timer = window.setInterval(() => void this.reload(root, true), REFRESH_MS);
	}

	private async reload(root: HTMLElement, silent = false): Promise<void> {
		const session = getSession();
		const driverId = session?.sub;
		if (!driverId) return;
		try {
			await registerDriver(session!.username).catch(() => {}); // idempotent
			const [available, mine] = await Promise.all([
				listAvailableDeliveries(),
				listMyDeliveries(driverId),
			]);
			if (this.destroyed) return;
			// Repaint only when data actually changed - avoids flicker on each poll.
			const snapshot = JSON.stringify([available, mine]);
			if (snapshot !== this.lastSnapshot) {
				this.lastSnapshot = snapshot;
				this.paint(root, available, mine);
			}
		} catch (error) {
			if (this.destroyed || silent) return;
			mount(
				root,
				h(
					"div",
					{ class: "view" },
					emptyState(
						"Could not load deliveries",
						error instanceof Error ? error.message : "Unknown error",
						h(
							"button",
							{ class: "btn btn-primary", onclick: () => void this.render(root) },
							"Retry",
						),
					),
				),
			);
		}
	}

	private paint(root: HTMLElement, available: Delivery[], mine: Delivery[]): void {
		const active = mine.filter((d) => ACTIVE_STATUSES.includes(d.status));
		const hasActive = active.length > 0;

		mount(
			root,
			h(
				"div",
				{ class: "view" },
				h(
					"div",
					{ class: "page-header" },
					h(
						"div",
						{},
						h("h1", {}, "Deliveries"),
						h("p", { class: "subtitle" }, "One active delivery at a time. Auto-refreshes."),
					),
					h("a", { class: "btn btn-secondary", href: "#/driver/earnings" }, "My earnings"),
				),
				h(
					"div",
					{ class: "section" },
					h("h2", {}, "My active delivery"),
					active.length === 0
						? h("p", { class: "muted" }, "No active delivery. Claim one below.")
						: h("div", { class: "stack" }, ...active.map((d) => this.activeCard(d))),
				),
				h(
					"div",
					{ class: "section" },
					h("h2", {}, `Available (${available.length})`),
					available.length === 0
						? h("p", { class: "muted" }, "Nothing available right now - check back soon.")
						: h(
								"div",
								{ class: "stack" },
								...available.map((d) => this.availableCard(root, d, hasActive)),
							),
				),
			),
		);
		void this.computeDistances(available);
		void this.computeRestaurantNames([...available, ...mine]);
	}

	private async computeRestaurantNames(deliveries: Delivery[]): Promise<void> {
		for (const d of deliveries) {
			if (this.destroyed) break;
			const el = document.getElementById(`resname-${d.deliveryId}`);
			if (!el) continue;
			const name = await restaurantNameForOrder(d.orderId);
			if (name && !this.destroyed) el.textContent = name;
		}
	}

	private async computeDistances(deliveries: Delivery[]): Promise<void> {
		for (const d of deliveries) {
			if (this.destroyed) break;
			const el = document.getElementById(`dist-${d.deliveryId}`);
			if (!el) continue;
			try {
				const [pickup, delivery] = await Promise.all([
					geocode(d.pickupAddress),
					geocode(d.deliveryAddress),
				]);
				if (pickup && delivery) {
					const km = haversineKm(pickup, delivery);
					const mins = Math.round((km / 30) * 60);
					el.textContent = `${km.toFixed(1)} km, ~${mins} min drive`;
				} else {
					el.textContent = "";
				}
			} catch {
				/* offline - ignore */
			}
		}
	}

	private activeCard(d: Delivery): HTMLElement {
		return h(
			"a",
			{ class: "card card-interactive delivery-card", href: `#/driver/deliveries/${d.deliveryId}` },
			h(
				"div",
				{ class: "split" },
				h(
					"div",
					{ class: "cluster" },
					h("strong", {}, `Order ${d.orderId.slice(0, 8)}`),
					deliveryBadge(d.status),
				),
			),
			h(
				"div",
				{ class: "addresses" },
				h(
					"div",
					{},
					h("div", { class: "addr-restaurant", id: `resname-${d.deliveryId}` }),
					h("div", { class: "addr-label" }, "Pickup"),
					d.pickupAddress,
				),
				h(
					"div",
					{},
					h("div", { class: "addr-label" }, "Deliver to"),
					d.deliveryAddress,
				),
			),
		);
	}

	private availableCard(root: HTMLElement, d: Delivery, hasActive: boolean): HTMLElement {
		return h(
			"div",
			{ class: "card delivery-card" },
			h(
				"div",
				{ class: "split" },
				h(
					"div",
					{},
					h(
						"div",
						{ class: "cluster" },
						h("strong", {}, `Order ${d.orderId.slice(0, 8)}`),
						deliveryBadge(d.status),
					),
					h(
						"div",
						{ class: "addresses" },
						h(
							"div",
							{},
							h("div", { class: "addr-restaurant", id: `resname-${d.deliveryId}` }),
							h("div", { class: "addr-label" }, "Pickup"),
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
						"p",
						{ class: "muted", style: "margin-top:var(--space-2);font-size:var(--text-sm)" },
						`~${money(d.estimatedPayoutMin)} - ${money(d.estimatedPayoutMax)} estimated`,
					),
					h("p", { id: `dist-${d.deliveryId}`, class: "help muted", style: "font-size:var(--text-xs)" }, ""),
				),
				h(
					"button",
					{
						class: "btn btn-primary",
						disabled: hasActive,
						title: hasActive ? "Finish your active delivery first" : "",
						onclick: (e: Event) =>
							void this.claim(root, e.target as HTMLButtonElement, d.deliveryId),
					},
					"Claim",
				),
			),
		);
	}

	private async claim(root: HTMLElement, btn: HTMLButtonElement, deliveryId: string): Promise<void> {
		const driverId = getSession()?.sub;
		if (!driverId) return;
		busyButton(btn, true);
		try {
			await claimDelivery(deliveryId, driverId);
			toast("Delivery claimed", "success");
			location.hash = `#/driver/deliveries/${deliveryId}`;
		} catch (error) {
			busyButton(btn, false);
			toast(error instanceof Error ? error.message : "Claim failed", "error");
			await this.reload(root, true);
		}
	}

	destroy(): void {
		this.destroyed = true;
		if (this.timer !== null) window.clearInterval(this.timer);
	}
}
