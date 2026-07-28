import type { Eur } from "./types.ts";

/**
 * Order lifecycle (order-service OrderStatus):
 * PENDING → PLACED → ACCEPTED → READY_FOR_PICKUP → PICKED_UP → DELIVERED
 * PLACED|ACCEPTED → CANCELLED; PLACED → REJECTED (restaurant decision, US22-25).
 */
export type OrderStatus =
	| "PENDING"
	| "PLACED"
	| "ACCEPTED"
	| "READY_FOR_PICKUP"
	| "PICKED_UP"
	| "DELIVERED"
	| "REJECTED"
	| "CANCELLED";

export type CheckoutResponse = {
	orderId: string;
	status: OrderStatus;
	paymentRef: string | null;
	redirectUrl: string | null;
};

export type OrderItem = {
	menuItemId: string;
	itemName: string;
	quantity: number;
	unitPrice: Eur;
};

export type OrderDetail = {
	orderId: string;
	customerId: string;
	restaurantId: string;
	customerName: string;
	deliveryAddress: string | null;
	customerEmail: string;
	totalAmount: Eur;
	currency: string;
	status: OrderStatus;
	paymentRef: string | null;
	paymentStatus: string | null;
	items: OrderItem[];
};

export type TrackingEvent = {
	type: string;
	occurredAt: string;
	payloadJson: string | null;
};

/** US21/US33 tracking read model - current state + lifecycle timestamps + events */
export type Tracking = {
	orderId: string;
	status: OrderStatus;
	rejectReason: string | null;
	placedAt: string | null;
	acceptedAt: string | null;
	readyAt: string | null;
	pickedUpAt: string | null;
	deliveredAt: string | null;
	events: TrackingEvent[];
};
