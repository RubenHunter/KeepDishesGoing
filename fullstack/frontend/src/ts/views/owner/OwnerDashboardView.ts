import {
	closeRestaurant,
	createRestaurant,
	getRestaurantDetail,
	getRestaurantStatus,
	listDishes,
	openRestaurant,
} from "../../api/restaurantApi.ts";
import { ApiError } from "../../api/http.ts";
import type { RestaurantDetail } from "../../domain/Restaurant.ts";
import { RESTAURANT_TYPE_LABELS } from "../../domain/Restaurant.ts";
import { rememberRestaurant, resolveOwnerRestaurantId } from "../../state/ownerRestaurant.ts";
import { listOrdersByRestaurant } from "../../api/orderApi.ts";
import { weeklyHoursEditor } from "../../presenter/openingHours.ts";
import { addressAutocomplete } from "../../presenter/addressAutocomplete.ts";
import {
	badge,
	busyButton,
	emptyState,
	field,
	openBadge,
	skeletonCards,
	toast,
} from "../../presenter/components.ts";
import { h, mount } from "../../presenter/dom.ts";
import type { View } from "../View.ts";

/** US1/US2/US3 (create), US5 (pending counter), US12 (manual open/close). */
export class OwnerDashboardView implements View {
	private destroyed = false;

	async render(root: HTMLElement): Promise<void> {
		mount(root, h("div", { class: "view" }, skeletonCards(3)));
		const restaurantId = await resolveOwnerRestaurantId();

		if (!restaurantId) {
			if (!this.destroyed) this.paintCreateForm(root);
			return;
		}

		try {
			const [detail, status, dishes, orders] = await Promise.all([
				getRestaurantDetail(restaurantId),
				getRestaurantStatus(restaurantId),
				listDishes(restaurantId).catch(() => []),
				listOrdersByRestaurant(restaurantId).catch(() => []),
			]);
			if (this.destroyed) return;
			const live = dishes.filter((d) => d.status === "PUBLISHED").length;
			const pending = dishes.filter((d) => d.status === "DRAFT").length;
			const pendingOrders = orders.filter((o) => o.status === "PLACED").length;
			this.paintDashboard(root, restaurantId, detail, status.open, live, pending, pendingOrders);
		} catch (error) {
			if (this.destroyed) return;
			if (error instanceof ApiError && error.status === 404) {
				this.paintCreateForm(root); // remembered id no longer exists
			} else {
				mount(
					root,
					h(
						"div",
						{ class: "view" },
						emptyState(
							"Could not load your restaurant",
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
	}

	private paintDashboard(
		root: HTMLElement,
		restaurantId: string,
		detail: RestaurantDetail,
		isOpen: boolean,
		liveCount: number,
		pendingCount: number,
		pendingOrders: number,
	): void {
		const toggleBtn = h(
			"button",
			{
				class: `btn ${isOpen ? "btn-danger" : "btn-primary"}`,
				onclick: (e: Event) =>
					void this.toggleOpen(root, restaurantId, isOpen, e.target as HTMLButtonElement),
			},
			isOpen ? "Close restaurant" : "Open restaurant",
		);

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
						h("div", { class: "cluster" }, h("h1", {}, detail.name), openBadge(isOpen)),
						h(
							"p",
							{ class: "subtitle" },
							[detail.restaurantType?.replaceAll("_", " ").toLowerCase(), detail.fullAddress]
								.filter(Boolean)
								.join(" · "),
						),
					),
					toggleBtn,
				),
				h(
					"div",
					{ class: "stat-grid" },
					stat(`${liveCount}/10`, "Dishes live (max 10)"),
					stat(String(pendingCount), "Pending drafts"),
				),
				h(
					"div",
					{ class: "grid-cards" },
					h(
						"a",
						{ class: "card card-interactive", href: "#/owner/menu" },
						h("h2", {}, "Menu editor"),
						h(
							"p",
							{ class: "muted" },
							"Draft, publish, unpublish, stock, schedule - all dish changes in one place.",
						),
						pendingCount > 0
							? h("p", { style: "margin-top:var(--space-2)" }, badge(`${pendingCount} pending`, "warning"))
							: null,
					),
					h(
						"a",
						{ class: "card card-interactive", href: "#/owner/orders" },
						h("h2", {}, "Incoming orders"),
						h(
							"p",
							{ class: "muted" },
							"Accept or reject within 5 minutes, mark orders ready for pickup.",
						),
						pendingOrders > 0
							? h("p", { style: "margin-top:var(--space-2)" }, badge(`${pendingOrders} new`, "danger"))
							: null,
					),
				),
			),
		);
	}

	private async toggleOpen(
		root: HTMLElement,
		restaurantId: string,
		isOpen: boolean,
		btn: HTMLButtonElement,
	): Promise<void> {
		busyButton(btn, true);
		try {
			if (isOpen) await closeRestaurant(restaurantId);
			else await openRestaurant(restaurantId);
			toast(isOpen ? "Restaurant is now closed" : "Restaurant is now open", "success");
			await this.render(root);
		} catch (error) {
			busyButton(btn, false);
			toast(error instanceof Error ? error.message : "Failed", "error");
		}
	}

	// ---------- Create restaurant (US2/US3) ----------

	private paintCreateForm(root: HTMLElement): void {
		const name = h("input", { class: "input", required: true });
		const email = h("input", { class: "input", type: "email", required: true });
		const logo = h("input", { class: "input", type: "url", required: true, placeholder: "https://…" });
		const fullAddress = h("input", {
			class: "input",
			required: true,
			placeholder: "Street 1, 2000 Antwerp, Belgium",
		});

		addressAutocomplete(root, fullAddress, (parsed) => {
			fullAddress.value = parsed.displayName;
		});
		const type = h(
			"select",
			{ class: "select", required: true },
			...Object.entries(RESTAURANT_TYPE_LABELS).map(([value, label]) =>
				h("option", { value }, label),
			),
		);
		const hours = weeklyHoursEditor();
		const hoursError = h("span", { class: "error", style: "display:none" }, "Pick at least one opening day.");

		const submitBtn = h(
			"button",
			{ class: "btn btn-primary", type: "submit" },
			"Create restaurant",
		);

		const form = h(
			"form",
			{
				onsubmit: (e: Event) => {
					e.preventDefault();
					if (!hours.valid()) {
						hoursError.style.display = "";
						return;
					}
					hoursError.style.display = "none";
					void this.submitCreate(root, submitBtn, {
						name: name.value,
						fullAddress: fullAddress.value,
						email: email.value,
						openingHours: hours.value(),
						logo: logo.value,
						restaurantType: type.value,
					});
				},
			},
			field("Restaurant name", name),
			field("Type", type, { help: "Drives the € price category shown to customers." }),
			field("Full address", fullAddress, { help: "Street, number, postal code, city, country." }),
			field("Contact e-mail", email),
			h("div", { class: "field" }, h("label", {}, "Opening hours"), hours.el, hoursError),
			field("Logo URL", logo),
			submitBtn,
		);

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
						h("h1", {}, "Create your restaurant"),
						h("p", { class: "subtitle" }, "One restaurant per owner - all fields required."),
					),
				),
				h("div", { class: "card form-column", style: "max-width:640px" }, form),
			),
		);
	}

	private async submitCreate(
		root: HTMLElement,
		btn: HTMLButtonElement,
		body: Parameters<typeof createRestaurant>[0],
	): Promise<void> {
		busyButton(btn, true);
		try {
			const id = await createRestaurant(body);
			rememberRestaurant(id);
			toast("Restaurant created", "success");
			await this.render(root);
		} catch (error) {
			busyButton(btn, false);
			toast(error instanceof Error ? error.message : "Creation failed", "error");
		}
	}

	destroy(): void {
		this.destroyed = true;
	}
}

function stat(value: string, label: string): HTMLElement {
	return h(
		"div",
		{ class: "card stat-tile" },
		h("div", { class: "stat-value" }, value),
		h("div", { class: "stat-label" }, label),
	);
}
