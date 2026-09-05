import type { Eur } from "./types.ts";

/** Server cart mirror (order-service CartResponse). Cart is one restaurant (US16). */
export type CartItem = {
	menuItemId: string;
	itemName: string;
	quantity: number;
	unitPrice: Eur;
	lineTotal: Eur;
};

export type ServerCart = {
	cartId: string;
	customerId: string;
	restaurantId: string | null;
	items: CartItem[];
	total: Eur;
	currency: string;
};

export function itemCount(cart: ServerCart | null): number {
	return cart?.items.reduce((sum, i) => sum + i.quantity, 0) ?? 0;
}
