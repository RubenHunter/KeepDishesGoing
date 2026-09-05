import { DISH_CATEGORY_LABELS, type Dish } from "../../../domain/Dish.ts";
import type { RestaurantDetail } from "../../../domain/Restaurant.ts";
import { dishBadge, dishImg } from "../../../presenter/components.ts";
import { h } from "../../../presenter/dom.ts";
import { money } from "../../../presenter/format.ts";

export function dishCard(
	restaurant: RestaurantDetail,
	dish: Dish,
	onAdd: (dish: Dish) => void,
): HTMLElement {
	const orderable = dish.status === "PUBLISHED" && restaurant.open;
	return h(
		"div",
		{ class: `card dish-card ${dish.status === "OUT_OF_STOCK" ? "dish-unavailable" : ""}` },
		dish.imageUrl ? dishImg(dish.imageUrl, dish.name) : null,
		h(
			"div",
			{},
			h(
				"div",
				{ class: "dish-name" },
				dish.name,
				dish.status === "OUT_OF_STOCK" ? dishBadge("OUT_OF_STOCK") : null,
				dish.category
					? h("span", { class: "badge badge-neutral" }, DISH_CATEGORY_LABELS[dish.category])
					: null,
			),
			h("p", { class: "dish-desc" }, dish.description),
		),
		h(
			"div",
			{ class: "dish-side" },
			h("span", { class: "price" }, money(dish.price.amount)),
			h(
				"button",
				{
					class: "btn btn-primary btn-sm",
					disabled: !orderable,
					onclick: () => onAdd(dish),
				},
				"Add",
			),
		),
	);
}
