import {
	addItem,
	clearCart as apiClearCart,
	createCart,
	getCart,
	removeItem,
	updateItemQuantity,
} from "../api/cartApi.ts";
import type { ServerCart } from "../domain/Cart.ts";
import type { Eur } from "../domain/types.ts";
import { load, remove, save } from "../infrastructure/storage.ts";
import { getSession } from "./session.ts";

/**
 * Server-backed cart state. Browser keeps only customerId + cartId;
 * the server is the source of truth (prices validated at checkout, US17).
 */

const CUSTOMER_KEY = "kdg.customerId";
const CART_KEY = "kdg.cartId";

let cart: ServerCart | null = null;
const listeners = new Set<() => void>();

export function customerId(): string {
	// Logged-in users are keyed by their Keycloak subject so order history
	// follows the account; guests fall back to a browser-local id.
	const sub = getSession()?.sub;
	if (sub) return sub;
	let id = load<string>(CUSTOMER_KEY);
	if (!id) {
		id = crypto.randomUUID();
		save(CUSTOMER_KEY, id);
	}
	return id;
}

export function currentCart(): ServerCart | null {
	return cart;
}

/** Load existing cart (page refresh) or create a fresh one lazily. */
export async function ensureCart(): Promise<ServerCart> {
	if (cart) return cart;
	const existingId = load<string>(CART_KEY);
	if (existingId) {
		try {
			cart = await getCart(existingId);
			notify();
			return cart;
		} catch {
			remove(CART_KEY); // stale cart id - start fresh
		}
	}
	cart = await createCart(customerId());
	save(CART_KEY, cart.cartId);
	notify();
	return cart;
}

/** US16 - one restaurant per cart. Returns false when cart holds another restaurant. */
export function cartRestaurantId(): string | null {
	return cart?.restaurantId ?? null;
}

/** Empties cart when switching restaurants (US16 hard rule). */
export async function resetCart(): Promise<ServerCart> {
	if (cart) {
		try {
			await apiClearCart(cart.cartId);
		} catch {
			/* cart already gone server-side */
		}
	}
	remove(CART_KEY);
	cart = await createCart(customerId());
	save(CART_KEY, cart.cartId);
	notify();
	return cart;
}

export async function add(
	restaurantId: string,
	item: { menuItemId: string; itemName: string; unitPrice: Eur },
): Promise<ServerCart> {
	const c = await ensureCart();
	cart = await addItem(c.cartId, { ...item, quantity: 1, restaurantId });
	notify();
	return cart;
}

export async function setQuantity(menuItemId: string, quantity: number): Promise<ServerCart> {
	const c = await ensureCart();
	cart = await updateItemQuantity(c.cartId, menuItemId, quantity);
	notify();
	return cart;
}

export async function removeLine(menuItemId: string): Promise<void> {
	if (!cart) return;
	await removeItem(cart.cartId, menuItemId);
	cart = await getCart(cart.cartId);
	notify();
}

export async function empty(): Promise<void> {
	if (!cart) return;
	try {
		await apiClearCart(cart.cartId);
	} catch {
		/* cart already cleared server-side (e.g. after checkout) */
	}
	remove(CART_KEY);
	cart = null;
	notify();
}

export async function refresh(): Promise<void> {
	if (!cart) return;
	cart = await getCart(cart.cartId);
	notify();
}

/** Drops the browser-local cart identity on logout so carts never leak across accounts. */
export function forgetLocalCart(): void {
	remove(CUSTOMER_KEY);
	remove(CART_KEY);
	cart = null;
	notify();
}

export function onCartChange(listener: () => void): () => void {
	listeners.add(listener);
	return () => listeners.delete(listener);
}

function notify(): void {
	for (const l of listeners) l();
}
