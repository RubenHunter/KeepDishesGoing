import type { DeliveryStatus } from "../domain/Delivery.ts";
import type { DishState } from "../domain/Dish.ts";
import type { OrderStatus } from "../domain/Order.ts";
import { h } from "./dom.ts";

// ---------- Badges (status = color + text, never color-only) ----------

type BadgeTone = "success" | "warning" | "danger" | "info" | "neutral" | "accent";

export function badge(label: string, tone: BadgeTone): HTMLElement {
	return h("span", { class: `badge badge-${tone}` }, label);
}

const ORDER_BADGES: Record<OrderStatus, { label: string; tone: BadgeTone }> = {
	PENDING: { label: "Processing", tone: "warning" },
	PLACED: { label: "Awaiting restaurant", tone: "warning" },
	ACCEPTED: { label: "Accepted", tone: "success" },
	REJECTED: { label: "Rejected", tone: "danger" },
	CANCELLED: { label: "Cancelled", tone: "neutral" },
	READY_FOR_PICKUP: { label: "Ready for pickup", tone: "info" },
	PICKED_UP: { label: "On its way", tone: "info" },
	DELIVERED: { label: "Delivered", tone: "success" },
};

export function orderBadge(status: OrderStatus): HTMLElement {
	return badge(ORDER_BADGES[status].label, ORDER_BADGES[status].tone);
}

const DELIVERY_BADGES: Record<DeliveryStatus, { label: string; tone: BadgeTone }> = {
	PENDING: { label: "Available", tone: "accent" },
	ASSIGNED: { label: "Claimed", tone: "warning" },
	READY_FOR_PICKUP: { label: "Ready for pickup", tone: "info" },
	PICKED_UP: { label: "Picked up", tone: "info" },
	IN_TRANSIT: { label: "In transit", tone: "info" },
	DELIVERED: { label: "Delivered", tone: "success" },
	CANCELLED: { label: "Cancelled", tone: "neutral" },
};

export function deliveryBadge(status: DeliveryStatus): HTMLElement {
	return badge(DELIVERY_BADGES[status].label, DELIVERY_BADGES[status].tone);
}

const DISH_BADGES: Record<DishState, { label: string; tone: BadgeTone }> = {
	PUBLISHED: { label: "Live", tone: "success" },
	OUT_OF_STOCK: { label: "Out of stock", tone: "warning" },
	DRAFT: { label: "Draft", tone: "neutral" },
};

export function dishBadge(state: DishState): HTMLElement {
	return badge(DISH_BADGES[state].label, DISH_BADGES[state].tone);
}

export function openBadge(isOpen: boolean): HTMLElement {
	return isOpen ? badge("Open", "success") : badge("Closed", "danger");
}

// ---------- Tag (info chip, not a status) ----------

export function tag(label: string): HTMLElement {
	return h("span", { class: "tag" }, label);
}

// ---------- Breadcrumb ----------

export function breadcrumb(items: { label: string; href?: string }[]): HTMLElement {
	const parts: (HTMLElement | string)[] = [];
	items.forEach((item, i) => {
		if (i > 0) parts.push(h("span", { class: "sep", "aria-hidden": "true" }, "/"));
		const isLast = i === items.length - 1;
		if (item.href && !isLast) {
			parts.push(h("a", { href: item.href }, item.label));
		} else {
			parts.push(h("span", { "aria-current": "page" }, item.label));
		}
	});
	return h("nav", { class: "breadcrumb", "aria-label": "Breadcrumb" }, ...parts);
}

// ---------- Logo with monogram fallback ----------

export function logoImg(
	url: string | null,
	name: string,
	className: string,
): HTMLElement {
	const monogram = h(
		"div",
		{
			class: className,
			style:
				"display:grid;place-items:center;font-weight:800;color:var(--accent);background:var(--accent-soft)",
			"aria-hidden": "true",
		},
		name.slice(0, 1).toUpperCase(),
	);
	if (!url) return monogram;
	const img = h("img", { class: className, src: url, alt: "", loading: "lazy" });
	img.addEventListener("error", () => img.replaceWith(monogram), { once: true });
	return img;
}

// ---------- Dish image with monogram fallback ----------

export function dishImg(url: string | null, name: string): HTMLElement {
	const monogram = h(
		"div",
		{
			class: "dish-img-fallback",
			style:
				"display:grid;place-items:center;font-weight:800;color:var(--accent);background:var(--accent-soft);border-radius:var(--radius-md);height:120px;width:160px",
			"aria-hidden": "true",
		},
		name.slice(0, 1).toUpperCase(),
	);
	if (!url) return monogram;
	const img = h("img", { class: "dish-img", src: url, alt: name, loading: "lazy", style: "border-radius:var(--radius-md);height:120px;width:160px;object-fit:cover" });
	img.addEventListener("error", () => img.replaceWith(monogram), { once: true });
	return img;
}

// ---------- Buttons ----------

export function busyButton(btn: HTMLButtonElement, busy: boolean): void {
	btn.disabled = busy;
	if (busy) {
		btn.dataset.label = btn.textContent ?? "";
		btn.replaceChildren(spinner());
	} else {
		btn.textContent = btn.dataset.label ?? btn.textContent;
	}
}

export function spinner(): HTMLElement {
	return h("span", { class: "spinner", role: "status", "aria-label": "Loading" });
}

// ---------- Skeletons ----------

export function skeletonCards(count: number): HTMLElement {
	return h(
		"div",
		{ class: "grid-cards" },
		...Array.from({ length: count }, () =>
			h("div", { class: "skeleton", style: "height: 120px" }),
		),
	);
}

export function skeletonLines(count: number): HTMLElement {
	return h(
		"div",
		{ class: "stack" },
		...Array.from({ length: count }, () =>
			h("div", { class: "skeleton", style: "height: 56px" }),
		),
	);
}

// ---------- Empty state ----------

export function emptyState(
	title: string,
	body: string,
	action?: HTMLElement,
): HTMLElement {
	return h("div", { class: "empty" }, h("h2", {}, title), h("p", {}, body), action ?? null);
}

// ---------- Toast ----------

export function toast(message: string, kind: "default" | "success" | "error" = "default"): void {
	const root = document.getElementById("toast-root");
	if (!root) return;
	const el = h(
		"div",
		{ class: `toast toast-${kind}`, role: "status" },
		message,
	);
	root.append(el);
	setTimeout(() => {
		el.classList.add("toast-leaving");
		el.addEventListener("transitionend", () => el.remove(), { once: true });
		setTimeout(() => el.remove(), 500); // fallback if no transition
	}, 4000);
}

// ---------- Modal (focus-trapped, Esc closes) ----------

export function modal(
	title: string,
	content: HTMLElement,
	actions: HTMLElement[],
	onClose: () => void = () => {},
): { close: () => void } {
	const close = (): void => {
		overlay.remove();
		document.removeEventListener("keydown", onKey);
		onClose();
	};
	const onKey = (e: KeyboardEvent): void => {
		if (e.key === "Escape") close();
	};
	const panel = h(
		"div",
		{ class: "modal", role: "dialog", "aria-modal": "true", "aria-label": title },
		h("h2", {}, title),
		content,
		h("div", { class: "modal-actions" }, ...actions),
	);
	const overlay = h("div", { class: "modal-overlay", onclick: (e: Event) => {
		if (e.target === overlay) close();
	} }, panel);
	document.addEventListener("keydown", onKey);
	document.body.append(overlay);
	const firstButton = panel.querySelector("button");
	if (firstButton instanceof HTMLElement) firstButton.focus();
	return { close };
}

// ---------- Form field ----------

export function field(
	labelText: string,
	input: HTMLElement,
	options: { help?: string; error?: string } = {},
): HTMLElement {
	const id = input.id || `f-${Math.random().toString(36).slice(2, 8)}`;
	input.id = id;
	const errorEl = options.error ? h("span", { class: "error" }, options.error) : null;
	if (options.error) {
		input.setAttribute("aria-invalid", "true");
	}
	return h(
		"div",
		{ class: "field" },
		h("label", { for: id }, labelText),
		input,
		options.help ? h("span", { class: "help" }, options.help) : null,
		errorEl,
	);
}

// ---------- Quantity stepper ----------

export function stepper(
	quantity: number,
	onChange: (next: number) => void,
	options: { min?: number; max?: number } = {},
): HTMLElement {
	const min = options.min ?? 0;
	const max = options.max ?? 99;
	const qty = h("span", { class: "qty mono" }, String(quantity));
	const change = (delta: number): void => {
		const next = quantity + delta;
		if (next < min || next > max) return;
		qty.classList.remove("pop");
		void qty.offsetWidth; // restart pop animation
		qty.classList.add("pop");
		onChange(next);
	};
	return h(
		"div",
		{ class: "stepper" },
		h(
			"button",
			{
				type: "button",
				"aria-label": "Decrease quantity",
				disabled: quantity <= min,
				onclick: () => change(-1),
			},
			"−",
		),
		qty,
		h(
			"button",
			{
				type: "button",
				"aria-label": "Increase quantity",
				disabled: quantity >= max,
				onclick: () => change(1),
			},
			"+",
		),
	);
}
