/** restaurant-service public DTO - full read model (list and detail share it). */
export type RestaurantStatus = "ACTIVE" | "INACTIVE";

export type RestaurantType =
	| "FAST_FOOD"
	| "BROODJESZAKEN"
	| "COMFORT_FOOD"
	| "FIJN_DINEREN"
	| "VISRESTAURANTS"
	| "MICHELIN_STER";

export const RESTAURANT_TYPE_LABELS: Record<RestaurantType, string> = {
	FAST_FOOD: "Fast food",
	BROODJESZAKEN: "Sandwich bar",
	COMFORT_FOOD: "Comfort food",
	FIJN_DINEREN: "Fine dining",
	VISRESTAURANTS: "Seafood",
	MICHELIN_STER: "Michelin star",
};

export type Restaurant = {
	id: string;
	name: string;
	status: RestaurantStatus;
	fullAddress: string | null;
	email: string | null;
	openingHours: string | null;
	logoUrl: string | null;
	restaurantType: RestaurantType | null;
};

/** order-service proxy detail - adds the computed open flag. */
export type RestaurantDetail = Omit<Restaurant, "status"> & {
	open: boolean;
};

/** US39 - price indication in € symbols */
export type PriceCategory = "€" | "€€" | "€€€" | "€€€€";

export function isOpen(r: Restaurant): boolean {
	return r.status === "ACTIVE";
}
