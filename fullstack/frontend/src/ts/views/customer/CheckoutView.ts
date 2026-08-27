import { checkout, confirmPayment, placeOrder } from "../../api/orderApi.ts";
import { rememberOrder } from "../../infrastructure/recentOrders.ts";
import { refreshMyOrders } from "../../state/myOrders.ts";
import { currentCart, empty, ensureCart } from "../../state/cart.ts";
import {
	badge,
	breadcrumb,
	busyButton,
	emptyState,
	field,
	toast,
} from "../../presenter/components.ts";
import { h, mount } from "../../presenter/dom.ts";
import { money } from "../../presenter/format.ts";
import { addressAutocomplete } from "../../presenter/addressAutocomplete.ts";
import { savedHomeAddress, savedProfile } from "../../state/homeAddress.ts";
import { getSession } from "../../state/session.ts";
import type { View } from "../View.ts";

/**
 * US17/US18 (server validates + locks at checkout), US19 (name/address/email),
 * US20 (payment provider - stubbed confirm step before placing).
 */
export class CheckoutView implements View {
	async render(root: HTMLElement): Promise<void> {
		let cart = currentCart();
		try {
			cart = await ensureCart();
		} catch {
			/* handled by empty state below */
		}
		if (!cart || cart.items.length === 0) {
			mount(
				root,
				h(
					"div",
					{ class: "view" },
					emptyState(
						"Nothing to check out",
						"Your cart is empty.",
						h("a", { class: "btn btn-primary", href: "#/" }, "Browse restaurants"),
					),
				),
			);
			return;
		}

		const profile = savedProfile();
		const home = savedHomeAddress();
		const session = getSession();

		// Prefill from the logged-in account, never from another user's saved profile.
		// Keycloak name/email are the fallback when no profile has been saved yet.
		const defaultName = profile?.name ?? session?.name ?? session?.username ?? "";
		const defaultEmail = profile?.email ?? session?.email ?? "";

		const nameInput = h("input", { class: "input", required: true, autocomplete: "name", value: defaultName });
		const emailInput = h("input", { class: "input", type: "email", required: true, autocomplete: "email", value: defaultEmail });
		const streetInput = h("input", { class: "input", required: true, autocomplete: "address-line1", value: home?.street ?? "" });
		const numberInput = h("input", { class: "input", required: true, autocomplete: "address-line2", value: home?.number ?? "" });
		const postalInput = h("input", { class: "input", required: true, autocomplete: "postal-code", value: home?.postalCode ?? "" });
		const cityInput = h("input", { class: "input", required: true, autocomplete: "address-level2", value: home?.city ?? "" });
		const countryInput = h("input", { class: "input", required: true, value: home?.country ?? "Belgium", autocomplete: "country-name" });

		addressAutocomplete(root, streetInput, (parsed) => {
			streetInput.value = parsed.street;
			numberInput.value = parsed.houseNumber;
			postalInput.value = parsed.postalCode;
			cityInput.value = parsed.city;
			countryInput.value = parsed.country || "Belgium";
		});

		const submitBtn = h(
			"button",
			{ class: "btn btn-primary", type: "submit" },
			`Continue to payment · ${money(cart.total)}`,
		);

		const form = h(
			"form",
			{
				onsubmit: (e: Event) => {
					e.preventDefault();
					void this.submit(root, submitBtn, {
						customerName: nameInput.value,
						email: emailInput.value,
						street: streetInput.value,
						number: numberInput.value,
						postalCode: postalInput.value,
						city: cityInput.value,
						country: countryInput.value,
					});
				},
			},
			field("Name", nameInput),
			field("Contact e-mail", emailInput),
			field("Street", streetInput),
			field("Number", numberInput),
			h(
				"div",
				{ class: "cluster", style: "align-items:flex-start" },
				h("div", { style: "flex:1;min-width:120px" }, field("Postal code", postalInput)),
				h("div", { style: "flex:2;min-width:160px" }, field("City", cityInput)),
			),
			field("Country", countryInput),
			submitBtn,
		);

		mount(
			root,
			h(
				"div",
				{ class: "view" },
				breadcrumb([
					{ label: "Restaurants", href: "#/" },
					{ label: "Cart", href: "#/cart" },
					{ label: "Checkout" },
				]),
				h("div", { class: "page-header" }, h("h1", {}, "Checkout")),
				h(
					"div",
					{ class: "menu-layout" },
					h("div", { class: "card form-column", style: "max-width:none" }, form),
					summaryPanel(),
				),
			),
		);
	}

	private async submit(
		root: HTMLElement,
		btn: HTMLButtonElement,
		form: {
			customerName: string;
			email: string;
			street: string;
			number: string;
			postalCode: string;
			city: string;
			country: string;
		},
	): Promise<void> {
		const cart = currentCart();
		if (!cart) return;
		busyButton(btn, true);

		try {
			const result = await checkout({
				cartId: cart.cartId,
				...form,
			});
			this.paintPayment(root, result.orderId, result.paymentRef, cart.total, form.customerName);
		} catch (error) {
			busyButton(btn, false);
			toast(error instanceof Error ? error.message : "Checkout failed", "error");
		}
	}

	/** US20 - stub payment step; confirming places the order (US23 window starts). */
	private paintPayment(
		root: HTMLElement,
		orderId: string,
		paymentRef: string | null,
		total: number,
		name: string,
	): void {
		const payBtn = h(
			"button",
			{ class: "btn btn-primary", style: "width:100%" },
			`Pay ${money(total)}`,
		);
		payBtn.addEventListener("click", () => {
			void (async () => {
				busyButton(payBtn, true);
				try {
					if (paymentRef) await confirmPayment(paymentRef);
					await placeOrder(orderId);
					rememberOrder(orderId);
					refreshMyOrders();
					await empty();
					location.hash = `#/orders/${orderId}/confirmation`;
				} catch (error) {
					busyButton(payBtn, false);
					toast(error instanceof Error ? error.message : "Payment failed", "error");
				}
			})();
		});

		mount(
			root,
			h(
				"div",
				{ class: "view" },
				h(
					"div",
					{ class: "card form-column", style: "margin:var(--space-12) auto" },
					h("div", { class: "cluster" }, h("h1", {}, "Payment"), badge("Demo stub", "info")),
					h(
						"p",
						{ class: "muted", style: "margin-block:var(--space-3)" },
						`${name}, your dishes are confirmed at current prices. Complete the payment to send your order to the restaurant.`,
					),
					h(
						"div",
						{ class: "row total", style: "display:flex;justify-content:space-between;margin-bottom:var(--space-4)" },
						h("strong", {}, "Total"),
						h("span", { class: "price" }, money(total)),
					),
					payBtn,
					h(
						"p",
						{ class: "help muted", style: "margin-top:var(--space-3)" },
						"Integrated payment provider (Mollie/Stripe) is stubbed in this demo.",
					),
				),
			),
		);
	}
}

function summaryPanel(): HTMLElement {
	const cart = currentCart();
	if (!cart) return h("aside");
	return h(
		"aside",
		{ class: "card cart-summary" },
		h("h2", {}, "Order summary"),
		...cart.items.map((line) =>
			h(
				"div",
				{ class: "row" },
				h("span", {}, `${line.quantity}× ${line.itemName}`),
				h("span", { class: "mono" }, money(line.lineTotal)),
			),
		),
		h(
			"div",
			{ class: "row total" },
			h("span", {}, "Total"),
			h("span", { class: "price" }, money(cart.total)),
		),
		h("p", {}, badge("Prices confirmed at placement", "info")),
	);
}
