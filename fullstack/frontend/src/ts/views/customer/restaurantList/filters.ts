import {
	isOpen,
	type PriceCategory,
	type Restaurant,
	type RestaurantType,
} from "../../../domain/Restaurant.ts";

/** A restaurant card enriched with its € price category (US39). */
export type CardModel = Restaurant & { priceCategory: PriceCategory | null };

export type SortKey = "recommended" | "distance" | "cost" | "name";

/** User-facing browse/filter/sort state for the restaurant list. */
export type FilterState = {
	openOnly: boolean;
	cuisine: string | null;
	search: string;
	sort: SortKey;
	maxKm: number;
	origin: string | null;
};

/** One entry in the quick-filter carousel. */
export type QuickFilter = {
	key: string;
	label: string;
	icon: string;
	matchType: RestaurantType | null;
	nameMatch: RegExp | null;
};

export const QUICK_FILTERS: QuickFilter[] = [
	{ key: "open", label: "Open now", icon: "clock.svg", matchType: null, nameMatch: null },
	{ key: "fastfood", label: "Fast food", icon: "burger.svg", matchType: "FAST_FOOD", nameMatch: null },
	{ key: "sandwich", label: "Sandwich bar", icon: "sandwich.svg", matchType: "BROODJESZAKEN", nameMatch: null },
	{ key: "comfort", label: "Comfort food", icon: "pizza.svg", matchType: "COMFORT_FOOD", nameMatch: null },
	{ key: "finedining", label: "Fine dining", icon: "finedining.svg", matchType: "FIJN_DINEREN", nameMatch: null },
	{ key: "seafood", label: "Seafood", icon: "seafood.svg", matchType: "VISRESTAURANTS", nameMatch: null },
	{ key: "michelin", label: "Michelin star", icon: "michelin.svg", matchType: "MICHELIN_STER", nameMatch: null },
	{ key: "chinese", label: "Chinese", icon: "noodles.svg", matchType: null, nameMatch: /chinese/i },
	{ key: "thai", label: "Thai", icon: "thai.svg", matchType: null, nameMatch: /thai/i },
	{ key: "sushi", label: "Sushi", icon: "sushi.svg", matchType: null, nameMatch: /sushi/i },
	{ key: "indian", label: "Indian", icon: "indian.svg", matchType: null, nameMatch: /indian/i },
	{ key: "mexican", label: "Mexican", icon: "taco.svg", matchType: null, nameMatch: /mexican|taco/i },
	{ key: "italian", label: "Italian", icon: "spaghetti.svg", matchType: null, nameMatch: /itali|pizza/i },
	{ key: "poke", label: "Poke bowls", icon: "pokebowl.svg", matchType: null, nameMatch: /poke/i },
];

/**
 * Narrow the card list by open/closed, cuisine, search text and max distance.
 * Distance is only enforced once that restaurant's km is known.
 */
export function applyFilters(
	cards: CardModel[],
	state: FilterState,
	km: Map<string, number>,
): CardModel[] {
	const q = state.search.trim().toLowerCase();
	const cuisine = QUICK_FILTERS.find((f) => f.key === state.cuisine) ?? null;

	return cards.filter((r) => {
		if (state.openOnly && !isOpen(r)) return false;
		if (cuisine?.matchType && r.restaurantType !== cuisine.matchType) return false;
		if (cuisine?.nameMatch) {
			const hay = `${r.name} ${r.fullAddress ?? ""}`;
			if (!cuisine.nameMatch.test(hay)) return false;
		}
		if (
			q !== "" &&
			!r.name.toLowerCase().includes(q) &&
			!(r.fullAddress ?? "").toLowerCase().includes(q)
		) {
			return false;
		}
		if (state.origin && km.has(r.id) && (km.get(r.id) ?? 0) > state.maxKm) {
			return false;
		}
		return true;
	});
}

export function sortCards(
	list: CardModel[],
	sort: SortKey,
	km: Map<string, number>,
): CardModel[] {
	const arr = [...list];
	switch (sort) {
		case "name":
			arr.sort((a, b) => a.name.localeCompare(b.name));
			break;
		case "cost":
			arr.sort(
				(a, b) =>
					(a.priceCategory?.length ?? 0) - (b.priceCategory?.length ?? 0) ||
					a.name.localeCompare(b.name),
			);
			break;
		case "distance":
			arr.sort(
				(a, b) =>
					(km.get(a.id) ?? Number.POSITIVE_INFINITY) -
						(km.get(b.id) ?? Number.POSITIVE_INFINITY) ||
					a.name.localeCompare(b.name),
			);
			break;
		default:
			arr.sort(
				(a, b) => Number(isOpen(b)) - Number(isOpen(a)) || a.name.localeCompare(b.name),
			);
	}
	return arr;
}
