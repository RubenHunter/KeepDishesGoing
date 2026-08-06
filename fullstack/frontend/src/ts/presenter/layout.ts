import { clearSession, getSession, hasRole, onSessionChange } from "../state/session.ts";
import { currentCart, onCartChange } from "../state/cart.ts";
import { lastTrackedOrderId } from "../state/trackedOrder.ts";
import { itemCount } from "../domain/Cart.ts";
import { h, mount } from "./dom.ts";
import { toast } from "./components.ts";

/**
 * App shell: sticky nav + page container. Nav re-renders on session/cart change.
 * Views render into #page.
 */
export function renderShell(): HTMLElement {
	const app = document.getElementById("app");
	if (!app) throw new Error("#app missing");
	const nav = h("header", { class: "app-nav" });
	const page = h("main", { class: "page container", id: "page" });
	mount(app, nav, page);
	const updateNav = (): void => renderNav(nav);
	onSessionChange(updateNav);
	onCartChange(updateNav);
	renderNav(nav);
	return page;
}

function renderNav(nav: HTMLElement): void {
	const session = getSession();
	const links: HTMLElement[] = [];
	const trackId = lastTrackedOrderId();

	// Common links for all authenticated users (or guests)
	if (session) {
		links.push(navLink("#/", "Restaurants"), cartLink());
		if (trackId) links.push(navLink(`#/orders/${trackId}/track`, "Track order"));
	}

	if (hasRole("owner")) {
		links.push(
			navLink("#/owner", "Dashboard"),
			navLink("#/owner/menu", "Menu"),
			navLink("#/owner/orders", "Orders"),
		);
	}
	if (hasRole("driver")) {
		links.push(
			navLink("#/driver", "Deliveries"),
			navLink("#/driver/earnings", "Earnings"),
		);
	}
	if (hasRole("admin")) {
		links.push(navLink("#/admin/payouts", "Payout reports"));
	}
	if (hasRole("user")) {
		links.push(navLink("#/account", "Account"));
	}

	if (!session) {
		links.push(navLink("#/", "Restaurants"), cartLink());
		if (trackId) links.push(navLink(`#/orders/${trackId}/track`, "Track order"));
	}

	if (session) {
		links.push(
			h(
				"button",
				{
					class: "btn btn-ghost btn-sm",
					onclick: () => {
						clearSession();
						toast("Logged out");
						location.hash = "#/";
					},
				},
				`Log out (${session.username})`,
			),
		);
	} else {
		links.push(navLink("#/user/login", "Sign in"));
	}

	mount(
		nav,
		h(
			"div",
			{ class: "container" },
			h(
				"a",
				{ class: "brand", href: "#/" },
				h("span", { class: "mark", "aria-hidden": "true" }, "K"),
				"Keep Dishes Going",
			),
			h("nav", { class: "nav-links", "aria-label": "Main" }, ...links),
		),
	);
}

function navLink(href: string, label: string): HTMLElement {
	const active = location.hash === href || (href !== "#/" && location.hash.startsWith(href));
	return h("a", { href, class: active ? "active" : "" }, label);
}

function cartLink(): HTMLElement {
	const count = itemCount(currentCart());
	return h(
		"a",
		{ href: "#/cart", class: "cart-link", "aria-label": `Cart, ${count} items` },
		"Cart",
		count > 0 ? h("span", { class: "cart-count" }, String(count)) : null,
	);
}

/** Refresh nav active state on route change. */
export function refreshNav(): void {
	const nav = document.querySelector(".app-nav");
	if (nav instanceof HTMLElement) renderNav(nav);
}
