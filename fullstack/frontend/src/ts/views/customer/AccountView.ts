import { getOrder } from "../../api/orderApi.ts";
import { getRestaurantDetail } from "../../api/restaurantApi.ts";
import { field, orderBadge, toast } from "../../presenter/components.ts";
import { h, mount } from "../../presenter/dom.ts";
import { money } from "../../presenter/format.ts";
import { getSession } from "../../state/session.ts";
import { recentOrderIds } from "../../infrastructure/recentOrders.ts";
import { saveProfile, savedProfile, type HomeAddress } from "../../state/homeAddress.ts";
import { addressAutocomplete } from "../../presenter/addressAutocomplete.ts";
import type { View } from "../View.ts";

interface OrderSummary {
	orderId: string;
	restaurantName: string;
	status: string;
	total: number;
	currency: string;
}

/** Customer account: profile (name, email, home address) + order history. */
export class AccountView implements View {
	private destroyed = false;

	async render(root: HTMLElement): Promise<void> {
		const session = getSession();
		mount(
			root,
			h("div", { class: "view" },
				h("div", { class: "page-header" },
					h("h1", {}, "My account"),
					session
						? h("p", { class: "subtitle" }, `Logged in as ${session.username}`)
						: null,
				),
				this.profileSection(root),
				this.orderHistorySection(),
			),
		);
	}

	private profileSection(root: HTMLElement): HTMLElement {
		const saved = savedProfile();

		const nameInput = h("input", { class: "input", value: saved?.name ?? "", placeholder: "Your full name", autocomplete: "name" });
		const emailInput = h("input", { class: "input", type: "email", value: saved?.email ?? "", placeholder: "you@example.com", autocomplete: "email" });
		const streetInput = h("input", { class: "input", value: saved?.address?.street ?? "", placeholder: "Street" });
		const numberInput = h("input", { class: "input", value: saved?.address?.number ?? "", placeholder: "Number", style: "max-width:100px" });
		const postalInput = h("input", { class: "input", value: saved?.address?.postalCode ?? "", placeholder: "Postal code", style: "max-width:120px" });
		const cityInput = h("input", { class: "input", value: saved?.address?.city ?? "", placeholder: "City" });
		const countryInput = h("input", { class: "input", value: saved?.address?.country ?? "Belgium", placeholder: "Country" });

		addressAutocomplete(root, streetInput, (parsed) => {
			streetInput.value = parsed.street;
			numberInput.value = parsed.houseNumber;
			postalInput.value = parsed.postalCode;
			cityInput.value = parsed.city;
			countryInput.value = parsed.country || "Belgium";
		});

		const saveBtn = h("button", { class: "btn btn-primary", type: "submit" }, "Save profile");

		const form = h("form", {
			onsubmit: (e: Event) => {
				e.preventDefault();
				const addr: HomeAddress = {
					street: streetInput.value,
					number: numberInput.value,
					postalCode: postalInput.value,
					city: cityInput.value,
					country: countryInput.value,
				};
				saveProfile({ name: nameInput.value, email: emailInput.value, address: addr });
				toast("Profile saved");
			},
		},
			field("Full name", nameInput),
			field("Contact e-mail", emailInput),
			field("Street", streetInput),
			h("div", { class: "cluster", style: "align-items:flex-start" },
				h("div", {}, field("Number", numberInput)),
				h("div", {}, field("Postal code", postalInput)),
			),
			h("div", { class: "cluster", style: "align-items:flex-start" },
				h("div", { style: "flex:1" }, field("City", cityInput)),
				h("div", { style: "flex:1" }, field("Country", countryInput)),
			),
			saveBtn,
		);

		return h("div", { class: "card", style: "margin-bottom:var(--space-6)" },
			h("h2", {}, "Your profile"),
			h("p", { class: "help muted", style: "margin-bottom:var(--space-4)" }, "Name and address auto-fill at checkout."),
			form,
		);
	}

	private orderHistorySection(): HTMLElement {
		const container = h("div", { class: "card" },
			h("h2", {}, "Order history"),
			h("p", { class: "help muted", style: "margin-bottom:var(--space-4)" }, "Recent orders placed from this browser."),
			h("div", { id: "order-history-list" }),
		);
		void this.loadOrderHistory(container.querySelector("#order-history-list")!);
		return container;
	}

	private async loadOrderHistory(listEl: HTMLElement): Promise<void> {
		const ids = recentOrderIds();
		if (ids.length === 0) {
			mount(listEl, h("p", { class: "muted" }, "No orders yet. Start by browsing restaurants."));
			return;
		}

		const orders: OrderSummary[] = [];
		for (const id of ids.slice(0, 15)) {
			try {
				const o = await getOrder(id);
				const name = await getRestaurantDetail(o.restaurantId)
					.then((r) => r.name)
					.catch(() => "Restaurant");
				orders.push({
					orderId: o.orderId,
					restaurantName: name,
					status: o.status,
					total: o.totalAmount,
					currency: o.currency,
				});
			} catch {
				/* order may no longer exist */
			}
		}

		if (this.destroyed) return;
		if (orders.length === 0) {
			mount(listEl, h("p", { class: "muted" }, "No recent orders found."));
			return;
		}

		mount(listEl,
			h("table", { class: "table" },
				h("thead", {},
					h("tr", {},
						h("th", {}, "Restaurant"),
						h("th", {}, "Status"),
						h("th", {}, "Total"),
						h("th", {}, ""),
					),
				),
				h("tbody", {},
					...orders.map((o) =>
						h("tr", {},
							h("td", {}, o.restaurantName),
							h("td", {}, orderBadge(o.status as Parameters<typeof orderBadge>[0])),
							h("td", { class: "mono" }, money(o.total)),
							h("td", {},
								h("a", { class: "btn btn-ghost btn-sm", href: `#/orders/${o.orderId}/track` }, "Track"),
							),
						),
					),
				),
			),
		);
	}

	destroy(): void {
		this.destroyed = true;
	}
}
