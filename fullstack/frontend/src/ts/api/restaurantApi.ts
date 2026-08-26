import { API } from "../config.ts";
import type { Dish, DishInput } from "../domain/Dish.ts";
import type {
	PriceCategory,
	Restaurant,
	RestaurantDetail,
	RestaurantStatus,
} from "../domain/Restaurant.ts";
import { request, requestRaw } from "./http.ts";

const restaurant = API.restaurant; // restaurant-service :8080
const proxy = API.order; // order-service :8081 public proxy

// ---------- Public (customer) ----------

export function listRestaurants(): Promise<Restaurant[]> {
	return request(`${restaurant}/restaurants`);
}

/** Rich detail via the order-service proxy (address, type, logo, open flag). */
export function getRestaurantDetail(id: string): Promise<RestaurantDetail> {
	return request(`${proxy}/restaurants/${id}`);
}

export function getPriceCategory(id: string): Promise<PriceCategory> {
	return request<{ symbol: PriceCategory }>(`${proxy}/restaurants/${id}/price-category`).then(
		(r) => r.symbol,
	);
}

/** Public menu - PUBLISHED + OUT_OF_STOCK dishes (drafts hidden). */
export function getMenu(restaurantId: string): Promise<Dish[]> {
	return request(`${restaurant}/restaurants/${restaurantId}/menu`);
}

export function getRestaurantStatus(
	id: string,
): Promise<{ open: boolean; status: RestaurantStatus; message: string }> {
	return request<{ isOpen?: boolean; open?: boolean; status: RestaurantStatus; message?: string }>(
		`${restaurant}/restaurants/${id}/status`,
	).then((r) => ({
		open: r.isOpen ?? r.open ?? r.status === "ACTIVE",
		status: r.status,
		message: r.message ?? "",
	}));
}

// ---------- Owner (role=owner) ----------

/** US1 - discover the restaurant owned by the current Keycloak subject (404 = none yet). */
export async function getMyRestaurantId(): Promise<string | null> {
	try {
		const r = await request<{ id: string }>(`${restaurant}/restaurants/mine`, { auth: true });
		return r.id;
	} catch {
		return null;
	}
}

export type CreateRestaurantRequest = {
	name: string;
	fullAddress: string;
	email: string;
	openingHours: string;
	logo: string;
	restaurantType: string;
};

/** US2/US3 - returns new restaurant id (parsed from the Location header). */
export async function createRestaurant(body: CreateRestaurantRequest): Promise<string> {
	const response = await requestRaw(`${restaurant}/restaurants`, {
		method: "POST",
		body,
		auth: true,
	});
	const location = response.headers.get("Location") ?? "";
	const id = location.split("/").filter(Boolean).pop();
	if (!id) throw new Error("Restaurant created but no id returned");
	return id;
}

export function openRestaurant(restaurantId: string): Promise<void> {
	return request(`${restaurant}/restaurants/${restaurantId}/open`, {
		method: "PATCH",
		auth: true,
	});
}

export function closeRestaurant(restaurantId: string): Promise<void> {
	return request(`${restaurant}/restaurants/${restaurantId}/close`, {
		method: "PATCH",
		auth: true,
	});
}

// ----- Menu management -----

export function listDishes(restaurantId: string): Promise<Dish[]> {
	return request(`${restaurant}/restaurants/${restaurantId}/dishes`);
}

/** US4 - create as draft; live menu unaffected until publish. */
export function createDish(restaurantId: string, body: DishInput): Promise<void> {
	return request(`${restaurant}/restaurants/${restaurantId}/dishes`, {
		method: "POST",
		body,
		auth: true,
	});
}

export function updateDish(restaurantId: string, dishId: string, body: DishInput): Promise<Dish> {
	return request(`${restaurant}/restaurants/${restaurantId}/dishes/${dishId}`, {
		method: "PUT",
		body,
		auth: true,
	});
}

/** US6 */
export function publishDish(restaurantId: string, dishId: string): Promise<void> {
	return request(`${restaurant}/restaurants/${restaurantId}/dishes/${dishId}/publish`, {
		method: "PATCH",
		auth: true,
	});
}

export function unpublishDish(restaurantId: string, dishId: string): Promise<void> {
	return request(`${restaurant}/restaurants/${restaurantId}/dishes/${dishId}/depublish`, {
		method: "PATCH",
		auth: true,
	});
}

/** US9 - stock toggle, immediate (never schedulable). */
export function setAvailability(
	restaurantId: string,
	dishId: string,
	available: boolean,
): Promise<void> {
	return request(`${restaurant}/restaurants/${restaurantId}/dishes/${dishId}/availability`, {
		method: "PATCH",
		body: { available },
		auth: true,
	});
}

/** US7 - apply all pending (draft) changes at once. */
export function publishMenu(restaurantId: string): Promise<void> {
	return request(`${restaurant}/restaurants/${restaurantId}/publish_menu`, {
		method: "POST",
		auth: true,
	});
}

/** US8 - schedule pending changes to go live together. publishAt: LocalDateTime */
export function schedulePublish(restaurantId: string, publishAt: string): Promise<void> {
	return request(`${restaurant}/restaurants/${restaurantId}/schedule_publish`, {
		method: "POST",
		body: { publishAt },
		auth: true,
	});
}

// ----- Order decisions (publish AMQP events; public on restaurant-service) -----

/** US22 - accept (delivery becomes claimable, US28). Pickup address resolved server-side from the restaurant record. */
export function acceptOrder(restaurantId: string, orderId: string, pickupAddress?: string, deliveryAddress?: string): Promise<void> {
	const body: Record<string, string> = {};
	if (pickupAddress) body.pickupAddress = pickupAddress;
	if (deliveryAddress) body.deliveryAddress = deliveryAddress;
	return request(`${restaurant}/restaurants/${restaurantId}/orders/${orderId}/accept`, {
		method: "POST",
		body,
		auth: true,
	});
}

/** US25 - reject with a reason shown to the customer. */
export function rejectOrder(restaurantId: string, orderId: string, reason: string): Promise<void> {
	return request(`${restaurant}/restaurants/${restaurantId}/orders/${orderId}/reject`, {
		method: "POST",
		body: { reason },
		auth: true,
	});
}

/** US26 - mark ready for pickup. */
export function markOrderReady(restaurantId: string, orderId: string): Promise<void> {
	return request(`${restaurant}/restaurants/${restaurantId}/orders/${orderId}/ready`, {
		method: "POST",
		auth: true,
	});
}
