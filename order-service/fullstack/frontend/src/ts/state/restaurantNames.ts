import { getOrder } from "../api/orderApi.ts";
import { getRestaurantDetail } from "../api/restaurantApi.ts";

/**
 * Resolves a restaurant name for a delivery's order id, with in-memory
 * caches (orderId → restaurantId, restaurantId → name) so polling views
 * don't repeat HTTP calls.
 */
const orderToRestaurant = new Map<string, string>();
const restaurantNames = new Map<string, string>();

export async function restaurantNameForOrder(orderId: string): Promise<string | null> {
	try {
		let restaurantId = orderToRestaurant.get(orderId);
		if (!restaurantId) {
			const order = await getOrder(orderId);
			restaurantId = order.restaurantId;
			orderToRestaurant.set(orderId, restaurantId);
		}
		let name = restaurantNames.get(restaurantId);
		if (!name) {
			name = (await getRestaurantDetail(restaurantId)).name;
			restaurantNames.set(restaurantId, name);
		}
		return name;
	} catch {
		return null;
	}
}
