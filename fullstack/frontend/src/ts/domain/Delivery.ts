import type { Eur } from "./types.ts";

/**
 * Delivery lifecycle (delivery-service DeliveryStatus):
 * PENDING (available, US28) → ASSIGNED (claimed, US27) → READY_FOR_PICKUP
 * → PICKED_UP → IN_TRANSIT → DELIVERED. CANCELLED = release claim (US29),
 * all via PATCH /deliveries/{id}/status.
 */
export type DeliveryStatus =
	| "PENDING"
	| "ASSIGNED"
	| "READY_FOR_PICKUP"
	| "PICKED_UP"
	| "IN_TRANSIT"
	| "DELIVERED"
	| "CANCELLED";

export type Delivery = {
	deliveryId: string;
	orderId: string;
	pickupAddress: string;
	deliveryAddress: string;
	deliveryPersonId: string | null;
	status: DeliveryStatus;
	estimatedPayoutMin: number;
	estimatedPayoutMax: number;
	assignedAt: string | null;
	readyAt: string | null;
	pickedUpAt: string | null;
	inTransitAt: string | null;
	deliveredAt: string | null;
	cancelledAt: string | null;
	cancellationReason: string | null;
};

export type PayoutRow = {
	payoutId: string;
	deliveryId: string;
	billableMinutes: number;
	currency: string;
	amount: Eur;
	readyAt: string;
	deliveredAt: string;
	computedAt: string;
};

/** US35 - payouts history + grand total; running total computed client-side */
export type DriverPayouts = {
	driverId: string;
	rows: PayoutRow[];
	totalCurrency: string;
	totalAmount: Eur;
};
