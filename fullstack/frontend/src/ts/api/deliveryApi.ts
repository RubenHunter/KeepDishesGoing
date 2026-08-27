import { API } from "../config.ts";
import type { Delivery, DriverPayouts } from "../domain/Delivery.ts";
import { download, request } from "./http.ts";

const base = API.delivery;

// ---------- Courier (role=driver). driverId = JWT subject. ----------

/** Idempotent self-registration - required once before payouts work (US35). */
export function registerDriver(name: string): Promise<void> {
	return request(`${base}/drivers`, {
		method: "POST",
		body: { name, vehicle: "bike" },
		auth: true,
	});
}

/** US28 - deliveries claimable right now (order accepted by restaurant). */
export function listAvailableDeliveries(): Promise<Delivery[]> {
	return request(`${base}/deliveries/available`, { auth: true });
}

/** Backend derives the driver from the JWT subject — no query param needed. */
export function listMyDeliveries(_driverId?: string): Promise<Delivery[]> {
	return request(`${base}/deliveries`, { auth: true });
}

export function getDelivery(deliveryId: string): Promise<Delivery> {
	return request(`${base}/deliveries/${deliveryId}`, { auth: true });
}

/**
 * US27-US30 lifecycle — one resource endpoint (PATCH /deliveries/{id}/status).
 * The backend always derives the driver id from the JWT subject.
 */
export function setDeliveryStatus(
	deliveryId: string,
	status: "ASSIGNED" | "CANCELLED" | "PICKED_UP" | "IN_TRANSIT" | "DELIVERED",
	reason?: string,
): Promise<void> {
	return request(`${base}/deliveries/${deliveryId}/status`, {
		method: "PATCH",
		body: { status, reason },
		auth: true,
	});
}

/** US27 - self-assign. One active delivery per courier (US31). */
export function claimDelivery(deliveryId: string): Promise<void> {
	return setDeliveryStatus(deliveryId, "ASSIGNED");
}

/** US29 - release claim, only while order not yet ready for pickup. */
export function cancelClaim(deliveryId: string, reason: string): Promise<void> {
	return setDeliveryStatus(deliveryId, "CANCELLED", reason);
}

export function markPickedUp(deliveryId: string): Promise<void> {
	return setDeliveryStatus(deliveryId, "PICKED_UP");
}

export function markInTransit(deliveryId: string): Promise<void> {
	return setDeliveryStatus(deliveryId, "IN_TRANSIT");
}

/** US30/US34 - final step; payout computed and credited. */
export function markDelivered(deliveryId: string): Promise<void> {
	return setDeliveryStatus(deliveryId, "DELIVERED");
}

/** US35 - completed deliveries + payouts + grand total. */
export function getPayouts(driverId: string): Promise<DriverPayouts> {
	return request(`${base}/drivers/${driverId}/payouts`, { auth: true });
}

// ---------- Admin (role=admin) ----------

/** US38 - payout report PDF for a date range (LocalDateTime params). */
export function downloadPayoutReport(from: string, to: string): Promise<void> {
	return download(
		`${base}/admin/payouts/report?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`,
		`payout-report-${from.slice(0, 10)}-${to.slice(0, 10)}.pdf`,
	);
}
